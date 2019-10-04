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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

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

        InputStream stream = new GZIPInputStream(new FileInputStream(pathToFile));
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.US_ASCII));

        int currentUserId = 0;
        int currentProductId = 0;
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            // Users
            // "review/userId: A1RSDE90N6RSZF" <- line format for user(it comes second)
            if (line.contains("review/userId")) {
                String amazonUserId = line.split(" ")[1];
                if (!userMap.containsKey(amazonUserId)) {
                    userMap.put(amazonUserId, nextUserId++);
                }
                currentUserId = userMap.get(amazonUserId);
            }

            // Products
            // product/productId: B00006HAXW <- line format for product (it comes first)
            if (line.contains("product/productId")) {
                String amazonProductId = line.split(" ")[1];
                if (!productMap.containsKey(amazonProductId)) {
                    productMap.put(amazonProductId, nextProductId++);
                }
                currentProductId = productMap.get(amazonProductId);
            }

            // Score
            // review/score: 5.0 <- line format for score (it comes last)
            if (line.contains("review/score")) {
                // Format for apache mahout -> user,product,score
                bw.write(currentUserId + "," + currentProductId + "," + line.split(" ")[1] + "\n");
                reviewCount++; // Each score is a new review
            }
        }
        bw.close();
        br.close();

        // Create an array of products to retrieve the product name with it's id later.
        products = new String[productMap.size()];
        for (Map.Entry<String, Integer> me : productMap.entrySet()) {
            products[me.getValue()] = me.getKey();
        }

        // Create recommender
        DataModel model = new FileDataModel(new File("dataset.csv"));
        UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
        UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, model);
        this.recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);

        temporaryCsv.delete();
    }

    List<String> getRecommendationsForUser(String user) throws TasteException {
        List<String> recommendations = new ArrayList<>();
        for (RecommendedItem recommendedItem : recommender.recommend(userMap.get(user), 3)) {
            recommendations.add(products[(int) recommendedItem.getItemID()]);
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
