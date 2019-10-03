package nearsoft.academy.bigdata.recommendation;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import java.io.*;
import java.util.*;

public class MovieRecommender {
    public static void main(String[] args) throws Exception {
        MovieRecommender recommender = new MovieRecommender("/Users/jsinohui/Documents/movies.txt");
        recommender.getRecommendationsForUser("A141HP4LYPWMSR");
    }

    private long nextUserId = 0;
    private long nextProductId = 0;
    private long reviewCount = 0;
    Map<String, Long> userMap = new HashMap<>();
    Map<String, Long> productMap = new HashMap<>();
    UserBasedRecommender recommender;
    String[] products;

    MovieRecommender(String pathToFile) throws IOException, TasteException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File("dataset.csv")));

        BufferedReader br = new BufferedReader(new FileReader(new File(pathToFile)));
        String csvLine = "";
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            // Users
            if (line.contains("review/userId")) {
                String username = line.split(" ")[1];
                Long userId;
                if(userMap.containsKey(username)){
                   userId = userMap.get(username);
                }else{
                    userId = nextUserId;
                    userMap.put(username, nextUserId++);
                }
                csvLine = userId + "," + csvLine;
            }

            // Products
            if (line.contains("product/productId")) {
                String product = line.split(" ")[1];
                Long productId;
                if(productMap.containsKey(product)){
                    productId = productMap.get(product);
                }else{
                    productId = nextProductId;
                    productMap.put(product, nextProductId++);
                }
                csvLine = productId + ",";
            }

            // Score
            if (line.contains("review/score")){
                csvLine += line.split(" ")[1] + "\n";
                bw.write(csvLine);
                reviewCount++;
            }
        }
        products = new String[productMap.size()];
        for(Map.Entry<String, Long> me : productMap.entrySet()){
            products[me.getValue().intValue()] = me.getKey();
        }

        bw.flush();

        DataModel model = new FileDataModel(new File("dataset.csv"));
        UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
        UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, model);
        this.recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);

    }

    List<String> getRecommendationsForUser(String user) throws TasteException {
        List<String> recommendations = new ArrayList<>();
        for(RecommendedItem ri : recommender.recommend(userMap.get(user), 5)){
            recommendations.add(products[(int)ri.getItemID()]);
        }
        return recommendations;
    }

    public long getTotalReviews() {
        return reviewCount;
    }

    public int getTotalProducts() {
        return productMap.size();
    }

    public int getTotalUsers() {
        return userMap.size();
    }
}
