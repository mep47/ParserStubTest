package voterheads.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import voterheads.CLArguments;
import voterheads.FilenameUrlPair;
import voterheads.Voterheads;
import voterheads.extractor.JsonEvent;

import com.google.gson.Gson;

public class Indexer
{
    private static final String cannotProcessHTML = "We apologize for the inconvenience, at this time we are unable to process this document.";

    private static final Logger logger            = Logger.getLogger(Indexer.class);

    private static final int MAXFRAGMENTS = 10;

    public static void createIndex(FilenameUrlPair pair, String folderPath)
    {
        logger.info("Creating index for file: " + pair.getFilename() + " url="
                + pair.getUrl());

        final int fileSizeLimit = 1000000;

        final File indexDir = new File(folderPath + "/index");
        InputStream textFileStream = null;

        Directory directory = null;
        IndexWriter indexWriter = null;
        try
        {
            directory = FSDirectory.open(indexDir);

            final Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_47);
            final IndexWriterConfig conf = new IndexWriterConfig(
                    Version.LUCENE_47, analyzer);
            indexWriter = new IndexWriter(directory, conf);
            indexWriter.deleteAll();

            final File textFile = new File(pair.getFilename());
            final Metadata metadata = new Metadata();
            final ContentHandler handler = new BodyContentHandler(fileSizeLimit);
            final ParseContext context = new ParseContext();
            final Parser parser = new AutoDetectParser();

            textFileStream = new FileInputStream(textFile);

            parser.parse(textFileStream, handler, metadata, context);

            final String text = handler.toString();

            final Document indexDoc = new Document();

            final Field pathField = new StringField("file", textFile.getPath(),
                    Field.Store.YES);
            indexDoc.add(pathField);

            /**
             * Create a field with term vector enabled
             */
            final FieldType type = new FieldType();
            type.setIndexed(true);
            type.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
            type.setStored(true);
            type.setStoreTermVectors(true);
            type.setTokenized(true);
            type.setStoreTermVectorOffsets(true);
            final Field field = new Field("content", text, type);// with term
                                                                 // vector
            // enabled
            /***/
            // TextField f =new TextField("ncontent", text, Field.Store.YES);
            // //without term vector
            /**
             * Add above two field to document
             */
            indexDoc.add(field);
            // indexDoc.add(f);

            indexWriter.addDocument(indexDoc);

        }
        catch (final IOException e)
        {
            logger.error(e.getStackTrace());
        }
        catch (final TikaException e)
        {
        	logger.error(e.getStackTrace());
        }
        catch (final SAXException e)
        {
        	logger.error(e.getStackTrace());
        }
        finally
        {
            try
            {
                textFileStream.close();
                indexWriter.commit();
                indexWriter.deleteUnusedFiles();
                indexWriter.close();
            }
            catch (final IOException e)
            {
                e.printStackTrace();
            }
        }

    }

    // public static List<FilenameUrlPair> createIndex(List<String>
    // differentUrls, String folderPath)
    // {
    // URL url = null;
    // String outFilename = null;
    // byte[] inputLine = new byte[1024];
    // int inputLength;
    //
    // List<FilenameUrlPair> downloadedPagesFilenames = new
    // ArrayList<FilenameUrlPair>();
    //
    // InputStream in = null;
    // FileOutputStream out = null;
    //
    // ContentStreamUpdateRequest up = null;
    //
    // int pos = folderPath.lastIndexOf("/");
    // String corename = folderPath.substring(pos+1);
    //
    // try {
    //
    // SolrServer server = new
    // HttpSolrServer("http://localhost:"+solrPort+"/solr/"+corename);
    //
    // try {
    // server.deleteByQuery( "*:*" );
    // } catch (Exception ex) {
    // // do nothing, happens if there is no previous index to delete
    // }
    //
    // for(String urlString: differentUrls)
    // {
    // // if(!urlString.contains("MeetingID=235"))
    // // {
    // // continue;
    // // }
    // System.out.println("#######createIndex####### urlString = "+urlString);
    // pos = urlString.lastIndexOf("/");
    // String filename = urlString.substring(pos+1).replace(" ", "");
    //
    // try {
    // url = new URL(urlString.replace(" ", "%20"));
    // URLConnection urlConnection = url.openConnection();
    // in = urlConnection.getInputStream();
    //
    // DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
    // Calendar cal = Calendar.getInstance();
    // String fileDate = dateFormat.format(cal.getTime());
    //
    // new File(folderPath+"/pages/").mkdir();
    // outFilename = folderPath+"/pages/DT_"+fileDate+"_"+filename;
    //
    // FilenameUrlPair pair = new FilenameUrlPair(outFilename, url.toString())
    // ; downloadedPagesFilenames.add(pair);
    //
    // out = new FileOutputStream(outFilename);
    // while((inputLength = in.read(inputLine)) != -1)
    // {
    // out.write(inputLine, 0, inputLength);
    // }
    // out.flush();
    // out.close();
    // in.close();
    //
    // NamedList<Object> result = server.request(up);
    //
    //
    // } catch (MalformedURLException mfe) {
    // System.out.println("=========Catch 1========");
    // logger.error("Malforded URL - "+urlString);
    // logger.error(mfe.getMessage(), mfe);
    // continue;
    // } catch(RemoteSolrException rse) {
    // System.out.println("=========Catch 2========");
    // logger.error("Received bad status from the server fur url: "+url);
    // logger.info(rse.getMessage(), rse);
    // continue;
    // } catch (Exception ex) {
    // System.out.println("=========Catch 3========");
    // logger.error(ex.getMessage(), ex);
    // continue;
    // }
    //
    // }
    // } catch (Exception e1) {
    // System.out.println("=========Catch 4========");
    // logger.fatal(e1.getMessage(), e1);
    // // System.exit(0);
    // } finally {
    // try {
    // if(in != null)
    // {
    // in.close();
    // }
    // if(out != null)
    // {
    // out.close();
    // }
    // } catch (IOException e) {
    // // TODO Auto-generated catch block
    // System.out.println("=========Catch 5========");
    // e.printStackTrace();
    // logger.fatal(e.getMessage(), e);
    // }
    // }
    //
    // return downloadedPagesFilenames;
    //
    // }

    public static String getFileExtension(String url)
    {
        if (url != null)
        {
            if (url.contains("."))
            {
                logger.info("URL:" + url);
                return url.substring(url.lastIndexOf(".") + 1);
            }
            else if (url.contains("/"))
            {
                return url.substring(url.lastIndexOf("/") + 1);
            }
            else
            {
                return "unknown";
            }

        }
        return "null";

    }

    public static QueryResult keyWordQuery(String folderPath, String keyWords)
    {


        final File indexDir = new File(folderPath + "/index");
        StringBuffer queryResults = new StringBuffer(500);

        final Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_47);

        // Build a Query object
        Query query = null;
        final int hitsPerPage = 10;
        int totalHits = 0;

        try
        {

            // query = new QueryParser(Version.LUCENE_47, "content", new
            // StandardAnalyzer(Version.LUCENE_47)).parse("Audit Funding reading");
            // testing
            query = new QueryParser(Version.LUCENE_47, "content",
                    new StandardAnalyzer(Version.LUCENE_47)).parse(keyWords);


            final IndexReader reader = DirectoryReader.open(FSDirectory
                    .open(indexDir));
            final IndexSearcher searcher = new IndexSearcher(reader);

            final TopScoreDocCollector collector = TopScoreDocCollector.create(
                    hitsPerPage, true);
            searcher.search(query, collector);

            final ScoreDoc[] hits = collector.topDocs().scoreDocs;

            final SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
            final Highlighter highlighter = new Highlighter(htmlFormatter,
                    new QueryScorer(query));
            final SimpleFragmenter fragmenter = new SimpleFragmenter();
            fragmenter.setFragmentSize(200);
            highlighter.setTextFragmenter(fragmenter);
            for (final ScoreDoc hit : hits)
            {
                final int id = hit.doc;
                final Document doc = reader.document(hit.doc);
                System.out.println(doc.get("file") + "  (" + hit.score + ")");

                // String text = doc.get("ncontent");
                //
                // TokenStream tokenStream =
                // TokenSources.getAnyTokenStream(searcher.getIndexReader(), id,
                // "ncontent", analyzer);
                // TextFragment[] frag;
                // frag = highlighter.getBestTextFragments(tokenStream, text,
                // false, 4);
                // for (int j = 0; j < frag.length; j++) {
                // if ((frag[j] != null) && (frag[j].getScore() > 0)) {
                // System.out.println((frag[j].toString()));
                // }
                // }
                // Term vector
                final String text = doc.get("content");
                final TokenStream tokenStream = TokenSources.getAnyTokenStream(
                        searcher.getIndexReader(), id, "content", analyzer);
                final TextFragment[] frag = highlighter.getBestTextFragments(
                        tokenStream, text, false, MAXFRAGMENTS);
                totalHits = frag.length;
                logger.info("");
                for (final TextFragment element : frag)
                {
                    if ((element != null) && (element.getScore() > 0))
                    {
                        System.out.println((element.toString()));
                        queryResults.append("<br />");
                        queryResults.append(element.toString());
                    }
                }
            }

        }
        catch (final ParseException e)
        {
            logger.error(e.getStackTrace());
            queryResults = null;
        }
        catch (final IOException e)
        {
        	logger.error(e.getStackTrace());
        }
        catch (final InvalidTokenOffsetsException e)
        {
            // TODO Auto-generated catch block
        	logger.error(e.getStackTrace());
        }
        catch (Exception ex)
        {
        	logger.error(ex.getStackTrace());
        }

        // List<String> noMatchUrls = new ArrayList<String>();
        //
        // int pos = folderPath.lastIndexOf("/");
        // String coreName = folderPath.substring(pos+1);
        //
        // StringBuffer htmlText = null;
        //
        // // Map<String,String> queryResultMap = new HashMap<String,String>();
        //
        // try {
        //
        // HttpSolrServer solr = new
        // HttpSolrServer("http://localhost:"+solrPort+"/solr/"+coreName);
        //
        // SolrQuery query = new SolrQuery();
        // query.setQuery(org.getKeyWords());
        // logger.info("Search words = "+org.getKeyWords());
        // query.setHighlight(true);
        // query.setHighlightSnippets(1000);
        // query.setHighlightFragsize(200);
        // query.setParam("hl.fl", "content");
        // query.setRows(100000);
        //
        // QueryResponse response = solr.query(query);
        //
        // boolean firstTime = true;
        //
        // Map<String,Map<String,List<String>>> highLighting =
        // response.getHighlighting();
        //
        // long numFound = response.getResults().getNumFound();
        // logger.info("Total number of results found  = "+numFound);
        // htmlText = new StringBuffer(300);
        // htmlText.append("<html><body>");
        // htmlText.append("<p>Key Words: "+org.getKeyWords()+"</p>");
        // firstTime = false;
        //
        // for(String url: differentUrls)
        // {
        // logger.info("different url = "+url);
        // int pos1 = url.lastIndexOf("/")+1;
        // if(pos1 != -1)
        // {
        // String filename = url.substring(pos1);
        // String urlString = url.toString().replace(" ", "%20");
        //
        // Map<String,List<String>> map = highLighting.get(filename);
        // if(map == null)
        // {
        // noMatchUrls.add(url);
        // logger.info("No keywords matched for "+filename);
        // continue;
        // }
        // htmlText.append("<a href=\""+url+"\">"+org.getName()+"</a>");
        // htmlText.append(" - "+filename+"<br />");
        //
        // for(String s1: map.keySet())
        // {
        //
        // List<String> list = map.get(s1);
        // for(String s2: list)
        // {
        // // System.out.println("string value="+s2);
        // String extractedKeyWords = extractKeyWords(s2);
        // String s3 = s2.replace("<em>", "<font color=\"red\">");
        // String s4 = s3.replace("</em>", "</font>");
        // htmlText.append("<p><b>"+extractedKeyWords+"</b> - "+s4+"</p>");
        //
        // // queryResultMap.put(extractedKeyWords,s4);
        // }
        // }
        // }
        // }
        //
        // logger.info("The size of noMatchUrls is: "+noMatchUrls.size());
        // if(noMatchUrls.size() > 0)
        // {
        // htmlText.append("<h4>Urls with no matching key words:</h4><p>");
        // for(String url: noMatchUrls)
        // {
        // htmlText.append(url+"<br />");
        // }
        // htmlText.append("</p>");
        // }
        //
        // htmlText.append("</body></html>");
        //
        // } catch (Exception e1) {
        // logger.fatal(e1.getMessage(), e1);
        // // System.exit(0);
        //
        // }
        //
        // String htmlStr = null;
        // if(htmlText != null)
        // {
        // htmlStr = htmlText.toString();
        // }

        QueryResult result = new QueryResult();
        if (queryResults == null)
        {
            result = null;
        }
        else
        {
            result.setQueryResult(queryResults.toString());
            result.setHitCount(totalHits);
        }

        return result;
    }

    public static void sendJSON(String url, QueryResult queryResult,
            String orgId, String statusString)
    {
        // 05-16-2014 14:00 -0400
        final Calendar cal = Calendar.getInstance();
        final Date date = cal.getTime();
        final SimpleDateFormat format1 = new SimpleDateFormat(
                "MM-dd-yyyy HH:mm Z");
        final String startTime = format1.format(date);

    	String sendJSONUrlString = CLArguments.getArgValue(CLArguments.RETURN_URL);
        String content = null;
        HttpResponse response = null;

        final StringBuffer jsonString = new StringBuffer(300);
        final Gson gson = new Gson();

        final JsonEvent jsonEvent = new JsonEvent();

        if (queryResult.getQueryResult() != null)
        {
            jsonEvent.setDescription(queryResult.getQueryResult());
        }
        else if (!getFileExtension(url).equalsIgnoreCase("pdf"))
        {
            jsonEvent.setDescription(cannotProcessHTML);
        }
        jsonEvent.setDoc_link(url);
        jsonEvent.setStart_time(startTime);
        jsonEvent.setTitle(statusString + " hit count "
                + queryResult.getHitCount());
        jsonEvent.setOrganization_ids(orgId);

        jsonString.append("{\"event\": ");
        jsonString.append(gson.toJson(jsonEvent));
        final int topicCount = 1;

        jsonString.append(",\"topic_count\":\"" + topicCount
                + "\",\"topics\": [");
        jsonString
                .append("{\"order\": \"1\",\"name\": \"test topic\",\"description\": \"test topic description\",\"topic_type\": \"Non-Voteable\",\"entity_ordinal\": \"A\"}]}");
        // jsonString.append("]}");
        // jsonString.append(",\"topic_count\":\""+topicCount+"\"}");

        final HttpClient httpClient = new DefaultHttpClient();
        final HttpPost httpPost = new HttpPost(sendJSONUrlString);

        httpPost.addHeader("Content-Type", "application/json");
        httpPost.addHeader("token", "583d42b69ae461f6e65a3577059ba06a");
        httpPost.addHeader("Authorization", "Token");
        try
        {
            final HttpEntity postEntity = new ByteArrayEntity(jsonString
                    .toString().getBytes("UTF-8"));
            httpPost.setEntity(postEntity);

        }
        catch (final UnsupportedEncodingException e)
        {
            // writing error to Log
            e.printStackTrace();
        }
        // Send the JSON string to the client
        try
        {
            //New event created so increment total events created
            Voterheads.incrementEventsCreated();
            response = httpClient.execute(httpPost);
            final HttpEntity respEntity = response.getEntity();

            if (respEntity != null)
            {
                // EntityUtils to get the response content
                content = EntityUtils.toString(respEntity);
            }
        }
        catch (final ClientProtocolException e)
        {
            // writing exception to log
            e.printStackTrace();
        }
        catch (final IOException e)
        {
            // writing exception to log
            e.printStackTrace();
        }
        finally
        {
            try
            {
                EntityUtils.consume(response.getEntity());
            }
            catch (final IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        System.out.println(jsonString);
        System.out.println("JSON response: " + content);

    }

}
