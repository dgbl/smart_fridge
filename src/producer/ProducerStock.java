package producer;

import store.*;
import java.util.ArrayList;
import thrift.*;

/**
 *
 * @author DNS
 */
public class ProducerStock {

    public static ArrayList<Article> pList = new ArrayList<Article>();

    public ProducerStock() {

    }

    public void initStock(int price1, int price2, int price3, int price4, int price5) {
        Article a1 = new Article();
        a1.name = "Apfel";
        a1.amount = 0;
        a1.price = price1;
        pList.add(a1);

        Article a2 = new Article();
        a2.name = "Banane";
        a2.amount = 0;
        a2.price = price2;
        pList.add(a2);

        Article a3 = new Article();
        a3.name = "Birne";
        a3.amount = 0;
        a3.price = price3;
        pList.add(a3);

        Article a4 = new Article();
        a4.name = "Mango";
        a4.amount = 0;
        a4.price = price4;
        pList.add(a4);

        Article a5 = new Article();
        a5.name = "Durian";
        a5.amount = 0;
        a5.price = price5;
        pList.add(a5);

    }

    public void deleteArticle(int i) {
        pList.remove(i);
    }

    public Article getArticle(int i) {
        return pList.get(i);
    }

    public void displayOffer() {
        for (int i = 0; i < pList.size(); i++) {
            System.out.println(pList.get(i).getName() + ": " + pList.get(i).getPrice() + " EUR");
        }
        System.out.println("----------------------------------------");
    }
}
