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

class MovieRecommender {
    private int reviewCount = 0;
    private Map<String, Integer> userMap = new HashMap<>();
    private Map<String, Integer> productMap = new HashMap<>();
    private UserBasedRecommender recommender;
    private String[] products;

    MovieRecommender(String pathToFile) throws IOException, TasteException {
        int nextUserId = 0;
        int nextProductId = 0;

        // Create temporary csv file to plug into mahout.
        File temporaryCsv = new File("dataset.csv");
        BufferedWriter bw = new BufferedWriter(new FileWriter(temporaryCsv));
        BufferedReader br = new BufferedReader(new FileReader(new File(pathToFile)));
        String csvLine = "";
        System.out.println("Reading movies.txt file");
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            // Users
//            "review/userId: A1RSDE90N6RSZF" <- line format for user(it comes second)
            if (line.contains("review/userId")) {
                String username = line.split(" ")[1];
                int userId;
                if(userMap.containsKey(username)){
                   userId = userMap.get(username);
                }else{
                    userId = nextUserId;
                    userMap.put(username, nextUserId++);
                }
                csvLine = userId + "," + csvLine;
            }

            // Products
//            product/productId: B00006HAXW <- line format for product (it comes first)
            if (line.contains("product/productId")) {
                String product = line.split(" ")[1];
                int productId;
                if(productMap.containsKey(product)){
                    productId = productMap.get(product);
                }else{
                    productId = nextProductId;
                    productMap.put(product, nextProductId++);
                }
                csvLine = productId + ",";
            }

            // Score
//            review/score: 5.0 <- line format for score (it comes last)
            if (line.contains("review/score")){
                csvLine += line.split(" ")[1] + "\n";
                bw.write(csvLine);
                reviewCount++; // Each score
            }
        }
        System.out.println("Finished reading movies.txt");

        // Create an array of products to retrieve the product name with it's id later.
        System.out.println("Constructing products array");
        products = new String[productMap.size()];
        for(Map.Entry<String, Integer> me : productMap.entrySet()){
            products[me.getValue()] = me.getKey();
        }

        // Create recommender
        DataModel model = new FileDataModel(new File("dataset.csv"));
        UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
        UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, model);
        this.recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);

        if(temporaryCsv.delete()){
            System.out.println("Deleted temporary dataset successfully");
        }else{
            System.out.println("Failed to delete temp dataset");
        }
    }

    List<String> getRecommendationsForUser(String user) throws TasteException {
        List<String> recommendations = new ArrayList<>();
        for(RecommendedItem ri : recommender.recommend(userMap.get(user), 5)){
            recommendations.add(products[(int)ri.getItemID()]);
        }
        return recommendations;
    }

    long getTotalReviews() {
        return reviewCount;
    }

    int getTotalProducts() {
        return productMap.size();
    }

    int getTotalUsers() {
        return userMap.size();
    }
}
