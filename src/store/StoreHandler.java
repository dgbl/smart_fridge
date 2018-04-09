package store;

import thrift.*;
import org.apache.thrift.TException;

public class StoreHandler implements Order.Iface {

    @Override
    public int calcPrice(Article article, int amount) {
        System.out.println("Bestellung: " + article.name + ": " + amount + ", zu je: " + article.price + " €");
        return amount * article.getPrice();
    }

    @Override
    public Article buyArticles(Article article, int price) {
        
        
        return null;
    }


}
