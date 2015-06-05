package co.zero.itext;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.Map;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Clase que sirve como prueba de concepto para la generación dinámica de Reportes
 * teniendo como base un template también en PDF
 * @author Hernan
 */
public class PdfFromTemplate extends PdfPageEventHelper{
	protected static final Color DEFAULT_TABLE_BORDER_COLOR = Color.decode("#6E6E6E");
	protected BaseColor defaultTableBorderBaseColor;
	protected static final int DEFAULT_SPACE_BETWEEN_TABLES = 5;
	
	protected Font font8BlackRegular = FontFactory.getFont("Times-Roman", 6f, Font.NORMAL);
	protected Font font8BlackBold = FontFactory.getFont("Times-Roman", 6f, Font.BOLD);
	protected Font fontGeneralTitle = FontFactory.getFont("Times-Roman", 12f, Font.BOLD);
	protected PdfWriter pdfWriter;
	protected Map<String, Object> parameters;
	private int portadaPageNumber, lastPageNumber = Integer.MAX_VALUE;
	
	/**
	 * Método que construye el reporte 
	 * @return
	 */
	public byte[] buildReport(Map<String, Object> parameters){
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Document document = null;
		byte[] pdfDocumentAsBytes = null;
		
		try{
			this.defaultTableBorderBaseColor = new BaseColor(DEFAULT_TABLE_BORDER_COLOR.getRed(), 
					DEFAULT_TABLE_BORDER_COLOR.getGreen(), DEFAULT_TABLE_BORDER_COLOR.getBlue());
			this.parameters = parameters;
			document = configurePDF(os, "Hernán", "El título del PDF");
			pdfWriter = PdfWriter.getInstance(document, os);
			pdfWriter.setPageEvent(this);
			document.open();
			
			addFirstPages(document, pdfWriter);
			addInfoToDocument(document);
			addLastPages(document, pdfWriter);
			
			document.close();
			pdfDocumentAsBytes = os.toByteArray();
			os.close();
			return pdfDocumentAsBytes;
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException(e.getMessage());
		}
	}
	
	/**
	 * Método que realiza las configuraciones básicas para el documento PDF
	 * @param os Stream de datso donde se podría escribir el archivo pdf
	 * @return Documento PDF con las configuraciones básicas realizadas
	 * @throws FileNotFoundException
	 * @throws DocumentException
	 */
	protected Document configurePDF(OutputStream os, String author, String title) throws FileNotFoundException, DocumentException{
		Document document = new Document(PageSize.LETTER);
		document.setMargins(30, 15, 70, 30);
		document.addTitle(title);
		document.addCreator(author);
		document.addAuthor(author);
		return document;
	}
	
	/**
	 * 
	 * @param document
	 * @param writer
	 * @throws IOException
	 * @throws DocumentException
	 */
	private void addFirstPages(Document document, PdfWriter writer) throws IOException, DocumentException{
		document.newPage();
		PdfReader reader = new PdfReader("Portada.pdf");
		portadaPageNumber = reader.getNumberOfPages();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PdfStamper stamper = new PdfStamper(reader, baos);
		stamper.setFormFlattening(true);
		stamper.getAcroFields().setField("employeeName", "Hernán Darío");
		stamper.getAcroFields().setField("EmployeeLastName", "Tenjo Mateus");
		stamper.close();
		reader.close();
		
		reader = new PdfReader(baos.toByteArray());
		baos.close();
		
		for(int i=1; i<= reader.getNumberOfPages(); i++){
			PdfImportedPage importedPage = writer.getImportedPage(reader, i);
			Image importedPageAsImage = Image.getInstance(importedPage);
			document.add(importedPageAsImage);			
		}
	}
	
	/**
	 * Metodo que gestiona las partes que deben ser agregadas al pdf
	 * @param document Documento al que se agregarán los elementos del reporte
	 * @throws DocumentException Si se presenta un error al agregar un elemento al documento
	 * @throws IOException 
	 * @throws MalformedURLException 
	 * @throws ParseException 
	 */
	protected void addInfoToDocument(Document document) throws DocumentException, MalformedURLException, IOException, ParseException{
		document.newPage();
		
		for(int i=0; i<150; i++){
			document.add(new Paragraph("Bueno este es un texto de prueba para verificar el buen funcionamiento del template"));
		}
	}
	
	/**
	 * 
	 * @param document
	 * @param writer
	 * @throws IOException
	 * @throws DocumentException
	 */
	private void addLastPages(Document document, PdfWriter writer) throws IOException, DocumentException{
		document.newPage();
		PdfReader reader = new PdfReader("EndPage.pdf");
		lastPageNumber = writer.getPageNumber();
		
		for(int i=1; i<= reader.getNumberOfPages(); i++){
			PdfImportedPage importedPage = writer.getImportedPage(reader, i);
			Image importedPageAsImage = Image.getInstance(importedPage);
			document.add(importedPageAsImage);			
		}
	}
	

	/* (non-Javadoc)
	 * @see com.lowagie.text.pdf.PdfPageEvent#onEndPage(com.lowagie.text.pdf.PdfWriter, com.lowagie.text.Document)
	 */
	@Override
	public void onEndPage(PdfWriter writer, Document document) {
		super.onStartPage(writer, document);
		int currentPage = writer.getPageNumber();

		try {
			if(portadaPageNumber < currentPage && currentPage < lastPageNumber){
				PdfReader pdfTemplate = new PdfReader("TemplateInfo.pdf");
				PdfImportedPage importedPage = writer.getImportedPage(pdfTemplate, 1);
				writer.getDirectContent().addTemplate(importedPage, 0, 0);
				pdfTemplate.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Método Main
	 */
	public static void main(String[] args) throws IOException, DocumentException {
		PdfFromTemplate template = new PdfFromTemplate();
		byte[] reportBytes = template.buildReport(null);
		FileOutputStream fos = new FileOutputStream("GeneratedReport.pdf");
		fos.write(reportBytes);
		fos.close();
	}
}