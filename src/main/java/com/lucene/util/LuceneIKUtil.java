package com.lucene.util;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.wltea.analyzer.lucene.IKAnalyzer;

import com.lucene.data.DataFactory;
import com.lucene.entity.Medicine;

public class LuceneIKUtil {
	private Directory directory ;
    private Analyzer analyzer ;
    
    /**
     * 带参数构造,参数用来指定索引文件目录
     * @param indexFilePath
     */
    public LuceneIKUtil(String indexFilePath){
        try {
            directory = FSDirectory.open(new File(indexFilePath));
            analyzer = new IKAnalyzer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    /**
     * 创建索引
     * Description：
     * @author dennisit@163.com Apr 3, 2013
     * @throws Exception
     */
    public void createIndex()throws Exception{
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_47,analyzer);
        IndexWriter indexWriter = new IndexWriter(directory,indexWriterConfig);
        indexWriter.deleteAll();
        List<Medicine> list = DataFactory.getInstance().getData();
        for(int i=0; i<list.size(); i++){
            Medicine medicine = list.get(i);
            Document document = addDocument(medicine.getId(), medicine.getName(), medicine.getFunction());
//            indexWriter.addDocument(document);//如果用添加，每次都是新的。所以需要用update
            indexWriter.updateDocument(new Term("id", medicine.getId().toString()), document);
        }
        
        indexWriter.close();
    }
    
    /**
     * 
     * Description：
     * @author dennisit@163.com Apr 3, 2013
     * @param id
     * @param title
     * @param content
     * @return
     */
    public Document addDocument(Integer id, String name, String function){
        Document doc = new Document();
        //Field.Index.NO 表示不索引         
        //Field.Index.ANALYZED 表示分词且索引         
        //Field.Index.NOT_ANALYZED 表示不分词但索引
//        doc.add(new Field("id",String.valueOf(id),Field.Store.YES,Field.Index.NOT_ANALYZED));
//        doc.add(new Field("name",name,Field.Store.YES,Field.Index.ANALYZED));
//        doc.add(new Field("function",function,Field.Store.YES,Field.Index.ANALYZED));
        FieldType ft = new FieldType();
        ft.setStored(true);
        ft.setIndexed(true);
        ft.setTokenized(false);
        doc.add(new Field("id",String.valueOf(id),ft));
        doc.add(new Field("name",name,TextField.TYPE_STORED));
        doc.add(new Field("function",function,TextField.TYPE_STORED));
        return doc;
    }
    
    /**
     * 
     * Description： 更新索引
     * @author dennisit@163.com Apr 3, 2013
     * @param id
     * @param title
     * @param content
     */
    public void update(Integer id,String title, String content){
        try {
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_47,analyzer);
            IndexWriter indexWriter = new IndexWriter(directory,indexWriterConfig);
            Document document = addDocument(id, title, content);
            Term term = new Term("id",String.valueOf(id));
            indexWriter.updateDocument(term, document);
            indexWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 
     * Description：按照ID进行索引
     * @author dennisit@163.com Apr 3, 2013
     * @param id
     */
    public void delete(Integer id){
        try {
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_47,analyzer);
            IndexWriter indexWriter = new IndexWriter(directory,indexWriterConfig);
            Term term = new Term("id",String.valueOf(id));
            indexWriter.deleteDocuments(term);
            indexWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 
     * Description：查询
     * @author dennisit@163.com Apr 3, 2013
     * @param where 查询条件
     * @param scoreDoc 分页时用
     */
    public List<Medicine> search(String[] fields,String keyword){
        
        IndexSearcher indexSearcher = null;
        List<Medicine> result = new ArrayList<Medicine>();
        
        
        try {
            //创建索引搜索器,且只读
//        	DirectoryReader ireader = DirectoryReader.open(directory);
//    		IndexSearcher isearcher = new IndexSearcher(ireader);
    		
            IndexReader indexReader = DirectoryReader.open(directory);
            indexSearcher = new IndexSearcher(indexReader);

            MultiFieldQueryParser queryParser =new MultiFieldQueryParser(Version.LUCENE_47, fields,analyzer);
            Query query = queryParser.parse(keyword);
            
            //返回前number条记录
            TopDocs topDocs = indexSearcher.search(query, 10);
            //信息展示
            int totalCount = topDocs.totalHits;
            System.out.println("共检索出 "+totalCount+" 条记录");
            
            
			// 高亮显示
			/*
			 * 创建高亮器,使搜索的结果高亮显示 SimpleHTMLFormatter：用来控制你要加亮的关键字的高亮方式 此类有2个构造方法
			 * 1：SimpleHTMLFormatter()默认的构造方法.加亮方式：<B>关键字</B>
			 * 2：SimpleHTMLFormatter(String preTag, String
			 * postTag).加亮方式：preTag关键字postTag
			 */
            Formatter formatter = new SimpleHTMLFormatter("<font color='red'>","</font>");    
			/*
			 * QueryScorer QueryScorer
			 * 是内置的计分器。计分器的工作首先是将片段排序。QueryScorer使用的项是从用户输入的查询中得到的；
			 * 它会从原始输入的单词、词组和布尔查询中提取项，并且基于相应的加权因子（boost factor）给它们加权。
			 * 为了便于QueryScoere使用，还必须对查询的原始形式进行重写。 比如，带通配符查询、模糊查询、前缀查询以及范围查询
			 * 等，都被重写为BoolenaQuery中所使用的项。
			 * 在将Query实例传递到QueryScorer之前，可以调用Query.rewrite
			 * (IndexReader)方法来重写Query对象
			 */
            QueryScorer fragmentScorer = new QueryScorer(query);
            Highlighter highlighter = new Highlighter(formatter, fragmentScorer);
            Fragmenter fragmenter = new SimpleFragmenter(100);
			/*
			 * Highlighter利用Fragmenter将原始文本分割成多个片段。
			 * 内置的SimpleFragmenter将原始文本分割成相同大小的片段，片段默认的大小为100个字符。这个大小是可控制的。
			 */
            highlighter.setTextFragmenter(fragmenter);
            
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            
            for(ScoreDoc scDoc : scoreDocs){
                Document  document = indexSearcher.doc(scDoc.doc);
                Integer id = Integer.parseInt(document.get("id"));
                String name = document.get("name");
                String function = document.get("function");
                //float score = scDoc.score; //相似度
                
                String lighterName = highlighter.getBestFragment(analyzer, "name", name);
                if(null==lighterName){
                    lighterName = name;
                }
                
                String lighterFunciton = highlighter.getBestFragment(analyzer, "function", function);
                if(null==lighterFunciton){
                    lighterFunciton = function;
                }
                
                Medicine medicine = new Medicine();
                
                medicine.setId(id);
                medicine.setName(lighterName);
                medicine.setFunction(lighterFunciton);
                
                result.add(medicine);
                            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        return result;
    }
    /**
     * 判断是否已经存在索引文件
     * @param indexPath
     * @return
     */
    private  boolean isExistIndexFile(String indexPath) throws Exception{
        File file = new File(indexPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        String indexSufix="/segments.gen";
         //根据索引文件segments.gen是否存在判断是否是第一次创建索引   
        File indexFile=new File(indexPath+indexSufix);
        return indexFile.exists();
    }
    
    
    

}
