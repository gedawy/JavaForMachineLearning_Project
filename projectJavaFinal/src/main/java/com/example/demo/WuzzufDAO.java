package com.example.demo;

import org.apache.commons.csv.CSVFormat;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import smile.data.DataFrame;
import smile.data.measure.NominalScale;
import smile.data.vector.IntVector;
import smile.data.vector.StringVector;
import smile.io.Read;

import javax.xml.crypto.dsig.keyinfo.KeyValue;
import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WuzzufDAO {

    private class KeyValDict{
        String name;
        long val;

        public KeyValDict(String name, long val) {
            this.name = name;
            this.val = val;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getVal() {
            return val;
        }

        public void setVal(long val) {
            this.val = val;
        }
    }


    private class ExpMinMax{
       Integer min;
       Integer max;

        public ExpMinMax(Integer min, Integer max) {
            this.min = min;
            this.max = max;
        }

        public Integer getMin() {
            return min;
        }

        public void setMin(Integer min) {
            this.min = min;
        }

        public Integer getMax() {
            return max;
        }

        public void setMax(Integer max) {
            this.max = max;
        }
    }

    private DataFrame df = null;
    private ArrayList<WuzzufPOJO> jobs = new ArrayList<WuzzufPOJO>();
    private List<WuzzufPOJO> tmpJobs = new ArrayList<WuzzufPOJO>();
    //data
    private List<Object> companies = null;
    private List<Object> titles = null;
    private List<Object> areas = null;


    //counts
    private List<KeyValDict> comJobCount = new ArrayList<KeyValDict>();
    private List<KeyValDict> titlesCounts = new ArrayList<KeyValDict>();
    private List<KeyValDict> locCounts = new ArrayList<KeyValDict>();
    private List<KeyValDict> skillsCounts = new ArrayList<KeyValDict>();
    private List<ExpMinMax> expYearsLimits = new ArrayList<ExpMinMax>();


    private List<String> jobSkills = new ArrayList<String>();
    private List<String> distinctSkills = new ArrayList<String>();



    public WuzzufDAO() {

        try {
            //read the dataset as dataframe
            CSVFormat format = CSVFormat.DEFAULT.withFirstRecordAsHeader();

            df = Read.csv("src/main/resources/Wuzzuf_Jobs.csv", format);

            System.out.println(df);

            //add the rows of dataframe to POJO object
            for (int i = 0; i < df.nrows(); i++) {
                jobs.add(new WuzzufPOJO(df.get(i, 0).toString(), df.get(i, 1).toString(), df.get(i, 2).toString(), df.get(i, 3).toString(), df.get(i, 4).toString(), df.get(i, 5).toString(), df.get(i, 6).toString(), df.get(i, 7).toString()));
            }


            //get the unique companies, job titles, and areas
            companies = df.stream().map(x -> x.get("Company")).distinct().collect(Collectors.toList());

            titles = df.stream().map(x -> x.get("Title")).distinct().collect(Collectors.toList());

            areas = df.stream().map(x -> x.get("Location")).distinct().collect(Collectors.toList());

            //loop through all unique companies and count jobs
            for (Object o : companies) {
                long jobCount = jobs.stream().filter(c -> c.getCompany().equals(o)).map(WuzzufPOJO::getTitle).count();
                comJobCount.add(new KeyValDict(o.toString(), jobCount));
            }

            comJobCount = comJobCount.stream().sorted(Comparator.comparingLong(KeyValDict::getVal).reversed()).collect(Collectors.toList());

            //loop through all unique titles and count titles
            for (Object o : titles) {
                long titleCount = jobs.stream().filter(t -> t.getTitle().equals(o)).map(WuzzufPOJO::getTitle).count();
                titlesCounts.add(new KeyValDict(o.toString(), titleCount));
            }

            titlesCounts = titlesCounts.stream().sorted(Comparator.comparingLong(KeyValDict::getVal).reversed()).collect(Collectors.toList());

            //loop through all unique locations and count locations
            for (Object o : areas) {
                long areaCount = jobs.stream().filter(a -> a.getLocation().equals(o)).map(WuzzufPOJO::getLocation).count();
                locCounts.add(new KeyValDict(o.toString(), areaCount));
            }

            locCounts = locCounts.stream().sorted(Comparator.comparingLong(KeyValDict::getVal).reversed()).collect(Collectors.toList());


            //process skills by each job
            df.stream().map(s -> s.get("Skills")).forEach(sk -> jobSkills.addAll(Arrays.asList(sk.toString().split(","))));
            //get the distinct job skills
            jobSkills.removeAll(Collections.singleton(null));

            distinctSkills = jobSkills.stream().distinct().collect(Collectors.toList());
            //count job skills
            System.out.println(distinctSkills);


            for (String skll : distinctSkills) {
                long skillCount = jobSkills.stream().filter(d -> d.equals(skll)).count();
                skillsCounts.add(new KeyValDict(skll, skillCount));
            }

            skillsCounts = skillsCounts.stream().sorted(Comparator.comparingLong(KeyValDict::getVal).reversed()).collect(Collectors.toList());

            df = df.merge(IntVector.of("YearsExpEncoded", encodeCat(df, "YearsExp")));
            df = df.merge(StringVector.of("ExpRange", getExpRange(df, "YearsExp")));
            System.out.println(df.slice(10,20));
            df = df.merge(IntVector.of("MinExpYears", encodeMinCat(df, "ExpRange")));
            //System.out.println(df);
            df = df.merge(IntVector.of("MaxExpYears", encodeMaxCat()));
        }
        catch(IOException | URISyntaxException e){
            e.printStackTrace();
        }
    }



    public DataFrame getDataFrame(){
        return df;
    }

    //Pie chart for top companies offering jobs
    public PieChart drawPieChart(int lmt){
        PieChart chart = new PieChartBuilder().width (800).height (600).title(getClass().getSimpleName()).build();

        Color[] sliceColors = new Color[]{new Color(235,189,52), new Color(138,204,45), new Color(45,204,201)};
        chart.getStyler().setSeriesColors(sliceColors);

        int lmtcount = 0;

        for(KeyValDict kv : comJobCount){
            chart.addSeries(kv.getName(), kv.getVal());

            lmtcount++;
            if(lmtcount == lmt) break;
        }

        return chart;
    }

    //job titles bar chart
    public CategoryChart drawJobsChart(int lmt){

        CategoryChart chart = new CategoryChartBuilder().width (1024).height (768).title ("Common Jobs").xAxisTitle ("Jobs").yAxisTitle ("Count").build ();
        // Customize Chart
        chart.getStyler().setLegendPosition (Styler.LegendPosition.InsideNW);
        chart.getStyler().setHasAnnotations (true);
        chart.getStyler().setStacked (true);
        // Series
        chart.addSeries("common wuzzuf jobs offered", titlesCounts.stream().map(KeyValDict::getName).limit(lmt).collect(Collectors.toList()), titlesCounts.stream().map(KeyValDict::getVal).limit(lmt).collect(Collectors.toList()));
        return chart;

    }

    //jobs areas bar chart
    public CategoryChart drawAreaChart(int lmt){
        CategoryChart chart = new CategoryChartBuilder().width (1024).height(768).title ("Common Areas").xAxisTitle ("Jobs").yAxisTitle ("Count").build ();
        // Customize Chart
        chart.getStyler ().setLegendPosition (Styler.LegendPosition.InsideNW);
        chart.getStyler ().setHasAnnotations (true);
        chart.getStyler ().setStacked (true);
        // Series
        chart.addSeries ("common wuzzuf jobs areas", locCounts.stream().map(KeyValDict::getName).limit(lmt).collect(Collectors.toList()), locCounts.stream().map(KeyValDict::getVal).limit(lmt).collect(Collectors.toList()));
        return chart;
    }



    //encoding
    public int[] encodeCat(DataFrame df, String colName){
        //convert to array of strings
        String[] values = df.stringVector(colName).distinct().toArray(new String[]{});
        //map to number scale and convert result to int
        int[] vals = df.stringVector(colName).factorize(new NominalScale(values)).toIntArray();

        return vals;
    }

    public String[] getExpRange(DataFrame df, String colName){
        return df.stringVector(colName).stream().map(x -> x.replace("Yrs of Exp","")).map(x -> x.replace("null","0")).collect(Collectors.toList()).toArray(new String[]{});
    }


    public ExpMinMax createExpObj(Object exp){
        Integer min = 0;
        Integer max = 0;

        if(exp.toString().contains("-")){
            String[] cleaned = exp.toString().replace(" ","").split("-");
            min = Integer.parseInt(cleaned[0]);
            max = Integer.parseInt(cleaned[1]);
        }else{
            String clean = exp.toString().replace(" ","").replace("+","");
            min = Integer.parseInt(clean);
            max = min+1;
        }

        return new ExpMinMax(min,max);
    }

    public IntStream encodeMinCat(DataFrame df, String colName){
        String[] values = df.stringVector(colName).stream().collect(Collectors.toList()).toArray(new String[]{});

        for(String val : values){
            expYearsLimits.add(createExpObj(val));
        }

        return expYearsLimits.stream().mapToInt(exp -> exp.getMin());

    }

    public IntStream encodeMaxCat(){
        return expYearsLimits.stream().mapToInt(exp -> exp.getMax());
    }


    //retrieve some data to display
    public String getSome(){
        return df.slice(0,20).toString();
    }

    //retrieve structure of dataframe
    public String getStructure(){
        return df.structure().toString();
    }

    //retrieve summary of dataframe
    public String getSummary(){
        return df.summary().toString();
    }


    //clean the dataset
    public String clean(){
        df = DataFrame.of(df.stream().filter(x -> !x.get("YearsExp").equals("null Yrs of Exp")).filter(x -> !x.get("Title").equals("null")).filter(x -> !x.get("Company").equals("null")).filter(x -> !x.get("Location").equals("null")).filter(x -> !x.get("Type").equals("null")).filter(x -> !x.get("Level").equals("null")).filter(x -> !x.get("Country").equals("null")).filter(x -> !x.get("Skills").equals("null")));
        df = df.omitNullRows();


        for (int i = 0; i < df.nrows(); i++) {
            tmpJobs.add(new WuzzufPOJO(df.get(i, 0).toString(), df.get(i, 1).toString(), df.get(i, 2).toString(), df.get(i, 3).toString(), df.get(i, 4).toString(), df.get(i, 5).toString(), df.get(i, 6).toString(), df.get(i, 7).toString()));
        }

        return df.omitNullRows().toString();
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
        Map<Object, Boolean> map = new HashMap<>();
        return t -> map.putIfAbsent(keyExtractor.apply( t ), Boolean.TRUE) == null;
    }


    public String clean_duplicate(){
        String text = "";

        tmpJobs = tmpJobs.stream().filter(distinctByKey(p -> p.getTitle() + " " + p.getCompany() +" "+p.getLocation() + " " + p.getType() +" " +p.getLevel() + " " + p.getCountry() +" "+p.getYearsExp() + " " + p.getSkills())).collect(Collectors.toList());

        System.out.println(tmpJobs.size());

        for(int i = 0; i < tmpJobs.size(); i++){
            text += "|"+tmpJobs.get(i).getTitle()+"|"+tmpJobs.get(i).getCompany()+"|"+tmpJobs.get(i).getLocation()+"|"+tmpJobs.get(i).getType()+"|"+tmpJobs.get(i).getLevel()+"|"+tmpJobs.get(i).getYearsExp()+"|"+tmpJobs.get(i).getCountry()+"|"+tmpJobs.get(i).getSkills()+"\n";
        }

        return text;
    }



    //methods for use on web

    //Display most demanding companies
    public String getDemandingCompanies(){
        String res = "";

        for(KeyValDict kv: comJobCount){
            res += "<p> company --> "+kv.getName()+" is offering "+kv.getVal()+" opportunities</p>";
        }
        return res;
    }


    //Display popular job titles
    public String getPopularTitles(){
        String res = "";

        for(KeyValDict kv: titlesCounts){
            res += "<p> Title --> "+kv.getName()+" has been offered "+kv.getVal()+" times</p>";
        }
        return res;
    }


    //Display popular areas
    public String getPopularAreas(){
        String res = "";

        for(KeyValDict kv: locCounts){
            res += "<p> Area --> "+kv.getName()+" had "+kv.getVal()+" jobs</p>";
        }
        return res;
    }

    //Display most important skills
    public String getImpSkills(){
        String res = "";

        for(KeyValDict kv: skillsCounts){
            res += "<p> Skill --> "+kv.getName()+" was mentioned "+kv.getVal()+" times</p>";
        }
        return res;
    }
}

