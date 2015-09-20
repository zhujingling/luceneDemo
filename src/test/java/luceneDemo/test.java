package luceneDemo;

import java.util.List;

import org.junit.Test;

import com.lucene.entity.Medicine;
import com.lucene.util.LuceneIKUtil;

public class test {
	@Test
	public void name() {
		  LuceneIKUtil luceneProcess = new LuceneIKUtil("G:\\WorkedCompany\\yanhuaProject\\lucene\\index");
	        try {
	            luceneProcess.createIndex();
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        //修改测试
	        luceneProcess.update(2, "测试内容", "修改测试。。。");
	        
	        //查询测试
	        String [] fields = {"name","function"};
	        List<Medicine> list = luceneProcess.search(fields,"感冒");
	        for(int i=0; i<list.size(); i++){
	            Medicine medicine = list.get(i);
	            System.out.println("("+medicine.getId()+")"+medicine.getName() + "\t" + medicine.getFunction());
	        }
	        //删除测试
	        //luenceProcess.delete(1);
	        
	    }

}
