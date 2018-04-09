package store;

import java.util.ArrayList;
import thrift.*;

/**
 *
 * @author DNS
 */
public class StoreStock {

    public static ArrayList<Article> aList = new ArrayList<Article>();

    public StoreStock() {

    }

    

    public void deleteArticle(int i) {
        aList.remove(i);
    }

    public Article getArticle(int i) {
        return aList.get(i);
    }
    
}
