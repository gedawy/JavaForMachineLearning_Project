package com.example.demo;

import org.knowm.xchart.BitmapEncoder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Logger;

@RestController
@RequestMapping(path = "api/gedawy")


@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);

	}


	@GetMapping("/hello")
	public String sayHello(@RequestParam(value="name", defaultValue="World") String name){
		return String.format ("Hello %s!", name);
	}

	//read wuzzuf dao object
	private WuzzufDAO wDAO = new WuzzufDAO();

	private String backButton = "<p><button onclick=\"location.href='http://localhost:8090/api/gedawy/read'\" type=\"button\" >Home</button></p>";


	//method to create HTML tables from data text
	private String createHTMLTable(String txt, String tableName, String btn){
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html>\n" +
				"<html>\n" +
				"<head>\n" +
				"<style>\n" +
				"table {\n" +
				"  width: 100%;\n" +
				"  font-family: arial, sans-serif;\n" +
				"}\n" +
				"\n" +
				"td, th {\n" +
				"  border: 1px solid #dddddd;\n" +
				"  text-align: left;\n" +
				"  padding: 8px;\n" +
				"}\n" +
				"\n" +
				"tr:nth-child(even) {\n" +
				"  background-color: #dddddd;\n" +
				"}\n" +
				"</style>\n" +
				"</head>\n" +
				"<body>\n");

		sb.append(btn);
		sb.append("<h1>"+tableName+"</h1>");
		sb.append("<table>");
		String[] strArr = txt.split("\n");

		for(String chunk: strArr){

			if(!(chunk.contains("--")||chunk.contains("["))){
				sb.append("<tr><td>"+ chunk.replace("|","</td><td>")+"</td></tr>");
			}
		}

		sb.append("</table>\n" +
				"\n" +
				"</body>\n" +
				"</html>");

		return sb.toString();
	}

	//statistics and displaying of basic data
	@GetMapping(path="structure")
	public String getStruct(){
		return createHTMLTable(wDAO.getStructure(),"Structure of Data", backButton);
	}

	@GetMapping(path="summary")
	public String getSummary(){
		return createHTMLTable(wDAO.getSummary(),"Summary of Data", backButton);
	}

	@GetMapping(path="clean")
	public String getCleaned(){
		return createHTMLTable(wDAO.clean(),"Cleaned version of Data", backButton);
	}

	@GetMapping(path="dup")
	public String getNoDuplicated(){
		return createHTMLTable(wDAO.clean_duplicate(),"No Duplicate version of Data", backButton);
	}

	@GetMapping(path="some")
	public String getSome(){
		return createHTMLTable(wDAO.getSome(),"Get some Data", backButton);
	}

	//displaying results of analysis (4)

	//get most demanding companies
	@GetMapping(path="companyDemand")
	public String getJobsDemand(){
		return backButton + wDAO.getDemandingCompanies();
	}

	//get most popular jobs
	@GetMapping(path="titles")
	public String getPopJobs(){
		return backButton + wDAO.getPopularTitles();
	}

	//get most popular areas
	@GetMapping(path="areas")
	public String getPopAreas(){
		return backButton + wDAO.getPopularAreas();
	}

	//get most imp. skills
	@GetMapping(path="skills")
	public String getImpSkills(){
		return backButton + wDAO.getImpSkills();
	}

	//VIZUALIZATIONS

	//get most demanding companies in a pie chart
	@GetMapping(path="piechart")
	public StringBuilder getPieChart(){

		String imgPath = "/images/jobsPieChart.png";
		try{
			BitmapEncoder.saveBitmap(wDAO.drawPieChart(5),"src/main/resources/static"+imgPath, BitmapEncoder.BitmapFormat.PNG);

		}catch(IOException e){
			e.printStackTrace();
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<h1>Demanding Jobs Pie Chart</h1>");
		sb.append("<img src='").append(imgPath).append("'/>");
		sb.append(backButton);
		return sb;
	}

	//get most popular titles in a barchart
	@GetMapping(path="titlesbar")
	public StringBuilder getTitlesChart(){

		String imgPath = "/images/titlesBarChart.png";
		try{
			BitmapEncoder.saveBitmap(wDAO.drawJobsChart(5),"src/main/resources/static"+imgPath, BitmapEncoder.BitmapFormat.PNG);

		}catch(IOException e){
			e.printStackTrace();
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<h1>Most Popular Job Titles</h1>");
		sb.append("<img src='").append(imgPath).append("'/>");
		sb.append(backButton);
		return sb;
	}

	//get most popular areas in a barchart
	@GetMapping(path="areasbar")
	public StringBuilder getAreasChart(){

		String imgPath = "/images/areasBarChart.png";
		try{
			BitmapEncoder.saveBitmap(wDAO.drawAreaChart(5),"src/main/resources/static"+imgPath, BitmapEncoder.BitmapFormat.PNG);

		}catch(IOException e){
			e.printStackTrace();
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<h1>Most Popular Job Areas</h1>");
		sb.append("<img src='").append(imgPath).append("'/>");
		sb.append(backButton);
		return sb;
	}

	@GetMapping(path = "read")
	public StringBuilder readData(){
		StringBuilder sb = new StringBuilder();
		sb.append("<h1>Welcome</h1>");
		//basic statistics
		sb.append("<p><button onclick=\"location.href='http://localhost:8090/api/gedawy/some'\" type=\"button\" >Read Some Data</button></p>");
		sb.append("<p><button onclick=\"location.href='http://localhost:8090/api/gedawy/clean'\" type=\"button\" >Clean Data</button></p>");
		sb.append("<p><button onclick=\"location.href='http://localhost:8090/api/gedawy/dup'\" type=\"button\" >Clean Duplicated</button></p>");
		sb.append("<p><button onclick=\"location.href='http://localhost:8090/api/gedawy/structure'\" type=\"button\" >Data Structure</button></p>");
		sb.append("<p><button onclick=\"location.href='http://localhost:8090/api/gedawy/summary'\" type=\"button\" >Data Summary</button></p>");
		//Insights
		sb.append("<p><button onclick=\"location.href='http://localhost:8090/api/gedawy/companyDemand'\" type=\"button\" >Most Demanding Companies</button></p>");
		sb.append("<p><button onclick=\"location.href='http://localhost:8090/api/gedawy/titles'\" type=\"button\" >Most Popular Jobs</button></p>");
		sb.append("<p><button onclick=\"location.href='http://localhost:8090/api/gedawy/areas'\" type=\"button\" >Most Popular Areas</button></p>");
		sb.append("<p><button onclick=\"location.href='http://localhost:8090/api/gedawy/skills'\" type=\"button\" >Most Important Skills</button></p>");
		//Charts
		sb.append("<p><button onclick=\"location.href='http://localhost:8090/api/gedawy/piechart'\" type=\"button\" >Most Demanding Companies Pie Chart</button></p>");
		sb.append("<p><button onclick=\"location.href='http://localhost:8090/api/gedawy/titlesbar'\" type=\"button\" >Most Popular Job Titles Bar Chart</button></p>");
		sb.append("<p><button onclick=\"location.href='http://localhost:8090/api/gedawy/areasbar'\" type=\"button\" >Most Popular Areas Bar Chart</button></p>");

		return sb;
	}

}
