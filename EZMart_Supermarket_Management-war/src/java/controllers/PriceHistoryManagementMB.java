package controllers;

import entityclass.ProductPriceHistory;
import entityclass.Products;
import entityclass.Users;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import sessionbeans.ProductPriceHistoryFacadeLocal;
import sessionbeans.ProductsFacadeLocal;
import sessionbeans.UsersFacadeLocal;

/**
 * Dynamic Price Management & Price Comparison Over Time Controller
 *
 * @author TRUONG LAM
 */
@Named(value = "priceHistoryMB")
@ViewScoped
public class PriceHistoryManagementMB implements Serializable {

    @EJB
    private ProductPriceHistoryFacadeLocal priceHistoryFacade;

    @EJB
    private ProductsFacadeLocal productsFacade;

    @EJB
    private UsersFacadeLocal usersFacade;

    private List<ProductPriceHistory> priceHistories;
    private List<ProductPriceHistory> filteredPriceHistories;
    private List<Products> allProducts;

    private ProductPriceHistory selectedPriceHistory;
    private Integer selectedProductId;

    // Filtering and comparison properties
    private Date startDate;
    private Date endDate;
    private Integer filterProductId;
    private String priceChangeType; // "INCREASE", "DECREASE", "ALL"

    // Price comparison data
    private List<PriceComparisonData> priceComparisonData;
    private Map<Integer, List<ProductPriceHistory>> productPriceHistoryMap;

    // Statistics
    private BigDecimal totalPriceIncrease;
    private BigDecimal totalPriceDecrease;
    private int totalPriceChanges;
    private BigDecimal averagePriceChange;

    @PostConstruct
    public void init() {
        loadPriceHistories();
        loadAllProducts();
        selectedPriceHistory = new ProductPriceHistory();
        initializeFilters();
        calculateStatistics();
    }

    public void loadPriceHistories() {
        priceHistories = priceHistoryFacade.findAll();
        filteredPriceHistories = new ArrayList<>(priceHistories);
        buildProductPriceHistoryMap();
    }

    public void loadAllProducts() {
        allProducts = productsFacade.findAll();
    }

    private void initializeFilters() {
        // Set default date range to last 30 days
        Calendar cal = Calendar.getInstance();
        endDate = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        startDate = cal.getTime();
        priceChangeType = "ALL";
    }

    private void buildProductPriceHistoryMap() {
        productPriceHistoryMap = priceHistories.stream()
                .filter(ph -> ph.getProductID() != null)
                .collect(Collectors.groupingBy(ph -> ph.getProductID().getProductID()));
    }

    public void applyFilters() {
        filteredPriceHistories = priceHistories.stream()
                .filter(this::matchesFilters)
                .collect(Collectors.toList());

        calculateStatistics();
        generatePriceComparisonData();
    }

    private boolean matchesFilters(ProductPriceHistory history) {
        // Date range filter
        if (startDate != null && history.getChangedAt() != null
                && history.getChangedAt().before(startDate)) {
            return false;
        }
        if (endDate != null && history.getChangedAt() != null
                && history.getChangedAt().after(endDate)) {
            return false;
        }

        // Product filter
        if (filterProductId != null && history.getProductID() != null
                && !history.getProductID().getProductID().equals(filterProductId)) {
            return false;
        }

        // Price change type filter
        if (!"ALL".equals(priceChangeType)) {
            boolean isIncrease = isPriceIncrease(history);
            if ("INCREASE".equals(priceChangeType) && !isIncrease) {
                return false;
            }
            if ("DECREASE".equals(priceChangeType) && isIncrease) {
                return false;
            }
        }

        return true;
    }

    private boolean isPriceIncrease(ProductPriceHistory history) {
        return history.getNewPrice() != null && history.getOldPrice() != null
                && history.getNewPrice().compareTo(history.getOldPrice()) > 0;
    }

    private void calculateStatistics() {
        totalPriceIncrease = BigDecimal.ZERO;
        totalPriceDecrease = BigDecimal.ZERO;
        totalPriceChanges = filteredPriceHistories.size();
        BigDecimal totalChange = BigDecimal.ZERO;

        for (ProductPriceHistory history : filteredPriceHistories) {
            if (history.getOldPrice() != null && history.getNewPrice() != null) {
                BigDecimal change = history.getNewPrice().subtract(history.getOldPrice());
                totalChange = totalChange.add(change);

                if (change.compareTo(BigDecimal.ZERO) > 0) {
                    totalPriceIncrease = totalPriceIncrease.add(change);
                } else if (change.compareTo(BigDecimal.ZERO) < 0) {
                    totalPriceDecrease = totalPriceDecrease.add(change.abs());
                }
            }
        }

        averagePriceChange = totalPriceChanges > 0
                ? totalChange.divide(BigDecimal.valueOf(totalPriceChanges), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    private void generatePriceComparisonData() {
        priceComparisonData = new ArrayList<>();

        if (filterProductId != null) {
            // Single product price history
            List<ProductPriceHistory> productHistory = productPriceHistoryMap.get(filterProductId);
            if (productHistory != null) {
                productHistory.stream()
                        .filter(this::matchesFilters)
                        .sorted((a, b) -> a.getChangedAt().compareTo(b.getChangedAt()))
                        .forEach(history -> {
                            PriceComparisonData data = new PriceComparisonData();
                            data.setDate(history.getChangedAt());
                            data.setPrice(history.getNewPrice());
                            data.setProductName(history.getProductID().getProductName());
                            data.setChangeReason(history.getChangeReason());
                            priceComparisonData.add(data);
                        });
            }
        } else {
            // Multiple products - show latest price for each
            Map<Integer, ProductPriceHistory> latestPrices = new HashMap<>();
            for (ProductPriceHistory history : filteredPriceHistories) {
                if (history.getProductID() != null) {
                    Integer productId = history.getProductID().getProductID();
                    ProductPriceHistory existing = latestPrices.get(productId);
                    if (existing == null || history.getChangedAt().after(existing.getChangedAt())) {
                        latestPrices.put(productId, history);
                    }
                }
            }

            for (ProductPriceHistory history : latestPrices.values()) {
                PriceComparisonData data = new PriceComparisonData();
                data.setDate(history.getChangedAt());
                data.setPrice(history.getNewPrice());
                data.setProductName(history.getProductID().getProductName());
                data.setChangeReason(history.getChangeReason());
                priceComparisonData.add(data);
            }
        }
    }

    public void prepareCreate() {
        selectedPriceHistory = new ProductPriceHistory();
        selectedProductId = null;
    }

    public void prepareEdit(ProductPriceHistory history) {
        selectedPriceHistory = history;
        selectedProductId = history.getProductID() != null ? history.getProductID().getProductID() : null;
    }

    public void savePriceHistory() {
        try {
            // Set product relationship
            if (selectedProductId != null) {
                Products product = productsFacade.find(selectedProductId);
                selectedPriceHistory.setProductID(product);

                // If old price is not set, get current product price
                if (selectedPriceHistory.getOldPrice() == null) {
                    selectedPriceHistory.setOldPrice(product.getUnitPrice());
                }
            }

            // Set current user as the one who changed the price
            // In a real application, this would come from the session
            Users currentUser = usersFacade.find(1); // Default to admin user
            selectedPriceHistory.setChangedBy(currentUser);

            // Set change timestamp if not set
            if (selectedPriceHistory.getChangedAt() == null) {
                selectedPriceHistory.setChangedAt(new Date());
            }

            if (selectedPriceHistory.getPriceHistoryID() == null) {
                priceHistoryFacade.create(selectedPriceHistory);
            } else {
                priceHistoryFacade.edit(selectedPriceHistory);
            }

            // Update the product's current price to the new price
            if (selectedPriceHistory.getNewPrice() != null) {
                Products product = selectedPriceHistory.getProductID();
                product.setUnitPrice(selectedPriceHistory.getNewPrice());
                productsFacade.edit(product);
            }

            loadPriceHistories();
            applyFilters();
            selectedPriceHistory = new ProductPriceHistory();
            selectedProductId = null;

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success",
                            "Price history record saved successfully"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",
                            "Failed to save price history: " + e.getMessage()));
        }
    }

    public void deletePriceHistory(ProductPriceHistory history) {
        try {
            priceHistoryFacade.remove(history);
            loadPriceHistories();
            applyFilters();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success",
                            "Price history record deleted successfully"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",
                            "Failed to delete price history: " + e.getMessage()));
        }
    }

    public String getPriceChangePercentage(ProductPriceHistory history) {
        if (history.getOldPrice() == null || history.getNewPrice() == null
                || history.getOldPrice().compareTo(BigDecimal.ZERO) == 0) {
            return "N/A";
        }

        BigDecimal change = history.getNewPrice().subtract(history.getOldPrice());
        BigDecimal percentage = change.divide(history.getOldPrice(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return percentage.setScale(2, RoundingMode.HALF_UP) + "%";
    }

    public String getPriceChangeDirection(ProductPriceHistory history) {
        if (history.getOldPrice() == null || history.getNewPrice() == null) {
            return "neutral";
        }

        int comparison = history.getNewPrice().compareTo(history.getOldPrice());
        return comparison > 0 ? "increase" : comparison < 0 ? "decrease" : "neutral";
    }

    public String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(date);
    }

    // Getters and Setters
    public List<ProductPriceHistory> getPriceHistories() {
        return priceHistories;
    }

    public void setPriceHistories(List<ProductPriceHistory> priceHistories) {
        this.priceHistories = priceHistories;
    }

    public List<ProductPriceHistory> getFilteredPriceHistories() {
        return filteredPriceHistories;
    }

    public void setFilteredPriceHistories(List<ProductPriceHistory> filteredPriceHistories) {
        this.filteredPriceHistories = filteredPriceHistories;
    }

    public List<Products> getAllProducts() {
        return allProducts;
    }

    public void setAllProducts(List<Products> allProducts) {
        this.allProducts = allProducts;
    }

    public ProductPriceHistory getSelectedPriceHistory() {
        return selectedPriceHistory;
    }

    public void setSelectedPriceHistory(ProductPriceHistory selectedPriceHistory) {
        this.selectedPriceHistory = selectedPriceHistory;
    }

    public Integer getSelectedProductId() {
        return selectedProductId;
    }

    public void setSelectedProductId(Integer selectedProductId) {
        this.selectedProductId = selectedProductId;

        // Auto-fill old price when product is selected
        if (selectedProductId != null && selectedPriceHistory != null && selectedPriceHistory.getOldPrice() == null) {
            Products product = productsFacade.find(selectedProductId);
            if (product != null) {
                selectedPriceHistory.setOldPrice(product.getUnitPrice());
            }
        }
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Integer getFilterProductId() {
        return filterProductId;
    }

    public void setFilterProductId(Integer filterProductId) {
        this.filterProductId = filterProductId;
    }

    public String getPriceChangeType() {
        return priceChangeType;
    }

    public void setPriceChangeType(String priceChangeType) {
        this.priceChangeType = priceChangeType;
    }

    public List<PriceComparisonData> getPriceComparisonData() {
        return priceComparisonData;
    }

    public void setPriceComparisonData(List<PriceComparisonData> priceComparisonData) {
        this.priceComparisonData = priceComparisonData;
    }

    public BigDecimal getTotalPriceIncrease() {
        return totalPriceIncrease;
    }

    public BigDecimal getTotalPriceDecrease() {
        return totalPriceDecrease;
    }

    public int getTotalPriceChanges() {
        return totalPriceChanges;
    }

    public BigDecimal getAveragePriceChange() {
        return averagePriceChange;
    }

    public String getPriceChartHtml() {
        if (priceComparisonData == null || priceComparisonData.isEmpty()) {
            return "<div class='text-center text-gray-500 py-8'>No price data available for the selected filters</div>";
        }

        StringBuilder html = new StringBuilder();
        html.append("<div class='price-chart-container'>");
        html.append("<div class='chart-header mb-4'>");
        html.append("<h4 class='text-lg font-semibold text-gray-800 dark:text-white'>Price Trend Chart</h4>");
        html.append("</div>");

        // Find min and max prices for scaling
        BigDecimal minPrice = priceComparisonData.stream()
                .map(PriceComparisonData::getPrice)
                .filter(p -> p != null)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxPrice = priceComparisonData.stream()
                .map(PriceComparisonData::getPrice)
                .filter(p -> p != null)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.valueOf(100));

        BigDecimal range = maxPrice.subtract(minPrice);
        if (range.compareTo(BigDecimal.ZERO) == 0) {
            range = BigDecimal.ONE;
        }

        html.append("<div class='chart-area' style='height: 300px; position: relative; border-left: 2px solid #e5e7eb; border-bottom: 2px solid #e5e7eb; padding: 20px;'>");

        // Draw grid lines
        for (int i = 0; i <= 5; i++) {
            BigDecimal gridValue = minPrice.add(range.multiply(BigDecimal.valueOf(i)).divide(BigDecimal.valueOf(5), 2, RoundingMode.HALF_UP));
            int yPos = 250 - (i * 50);
            html.append(String.format("<div class='grid-line' style='position: absolute; left: 0; right: 0; top: %dpx; border-top: 1px dashed #e5e7eb;'></div>", yPos));
            html.append(String.format("<div class='grid-label' style='position: absolute; left: -50px; top: %dpx; font-size: 12px; color: #6b7280;'>$%.2f</div>", yPos - 8, gridValue));
        }

        // SVG for chart lines and points
        html.append("<svg width='100%' height='300' style='position: absolute; top: 20px; left: 20px;'>");

        // Draw data points and lines
        int dataSize = priceComparisonData.size();
        for (int i = 0; i < dataSize; i++) {
            PriceComparisonData data = priceComparisonData.get(i);
            if (data.getPrice() == null) {
                continue;
            }

            BigDecimal normalizedPrice = data.getPrice().subtract(minPrice).divide(range, 4, RoundingMode.HALF_UP);
            int xPos = 30 + (i * (700 - 60) / Math.max(1, dataSize - 1));
            int yPos = 250 - (int) (normalizedPrice.doubleValue() * 250);

            // Draw line to next point
            if (i < dataSize - 1) {
                PriceComparisonData nextData = priceComparisonData.get(i + 1);
                if (nextData.getPrice() != null) {
                    BigDecimal nextNormalized = nextData.getPrice().subtract(minPrice).divide(range, 4, RoundingMode.HALF_UP);
                    int nextXPos = 30 + ((i + 1) * (700 - 60) / Math.max(1, dataSize - 1));
                    int nextYPos = 250 - (int) (nextNormalized.doubleValue() * 250);

                    html.append(String.format("<line x1='%d' y1='%d' x2='%d' y2='%d' stroke='#4e9767' stroke-width='2' stroke-linecap='round'></line>", xPos, yPos, nextXPos, nextYPos));
                }
            }

            // Draw data point
            html.append(String.format("<circle cx='%d' cy='%d' r='4' fill='#4e9767' stroke='white' stroke-width='2'></circle>", xPos, yPos));
        }

        html.append("</svg>");

        // Tooltips (HTML divs)
        int dataSize2 = priceComparisonData.size();
        for (int i = 0; i < dataSize2; i++) {
            PriceComparisonData data = priceComparisonData.get(i);
            if (data.getPrice() == null) {
                continue;
            }

            BigDecimal normalizedPrice = data.getPrice().subtract(minPrice).divide(range, 4, RoundingMode.HALF_UP);
            int xPos = 50 + (i * (700 - 100) / Math.max(1, dataSize2 - 1));
            int yPos = 250 - (int) (normalizedPrice.doubleValue() * 250);

            String tooltipText = String.format("%s: $%.2f<br>%s",
                    data.getProductName(),
                    data.getPrice(),
                    formatDate(data.getDate()));

            String safeTooltip = tooltipText
                    .replace("\"", "&quot;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");

            html.append(String.format(
                    "<div class=\"tooltip\" style=\"position:absolute; left:%dpx; top:%dpx; background:rgba(0,0,0,0.8); color:white; padding:5px 10px; border-radius:4px; font-size:12px; display:none; pointer-events:none; z-index:10; white-space:nowrap;\" data-tooltip=\"%s\"></div>",
                    xPos - 50,
                    yPos - 40,
                    safeTooltip
            ));

        }

        html.append("</div>");

        // Legend
        html.append("<div class='chart-legend mt-4 flex justify-center'>");
        html.append("<div class='legend-item flex items-center mr-4'>");
        html.append("<div class='legend-color' style='width: 12px; height: 12px; background: #4e9767; border-radius: 50%; margin-right: 8px;'></div>");
        html.append("<span class='text-sm text-gray-600 dark:text-gray-400'>Price Trend</span>");
        html.append("</div>");
        html.append("</div>");

        html.append("</div>");

        return html.toString();
    }

    // Inner class for price comparison data
    public static class PriceComparisonData implements Serializable {

        private Date date;
        private BigDecimal price;
        private String productName;
        private String changeReason;

        // Getters and setters
        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public String getChangeReason() {
            return changeReason;
        }

        public void setChangeReason(String changeReason) {
            this.changeReason = changeReason;
        }
    }
}
