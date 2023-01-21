package edu.sunderland.babybuy.products;

public class Product {
    String docId;
    String name;
    String description;
    String price;
    String purchased = "Not purchased";
    String filePath;
    String userId;

    public Product() { }

    public Product(
            String name,
            String description,
            String price,
            String purchased,
            String filePath,
            String userId
    ) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.purchased = purchased;
        this.filePath = filePath;
        this.userId = userId;
    }

    public String getUserId() {
        return this.userId;
    }

    public String getPrice() {
        return price;
    }

    public String getPurchased() { return purchased; }

    public String getFilePath() {
        return filePath;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
}