package org.cloudsimplus.abc;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Dynamic Performance Analyzer for Load Balancing Algorithms
 * Generates charts based on actual simulation results
 */
public class DynamicPerformanceAnalyzer {
    
    public static class AlgorithmResults {
        String name;
        int cloudletsProcessed;
        double avgExecutionTime;
        double slowestTime;
        double fastestTime;
        double efficiency;
        Map<Long, Integer> vmDistribution;
        double throughput;
        double responseTime;
        int peakConcurrentTransactions;
        
        AlgorithmResults(String name) {
            this.name = name;
            this.vmDistribution = new HashMap<>();
        }
    }
    
    private static List<AlgorithmResults> results = new ArrayList<>();
    // Keep a persistent history across test iterations in a single program run
    private static final List<AlgorithmResults> history = new ArrayList<>();
    
    public static void addResults(String algorithmName, int cloudletsProcessed, 
                                double avgExecutionTime, double slowestTime, double fastestTime,
                                double efficiency, Map<Long, Integer> vmDistribution,
                                int peakConcurrentTransactions) {
        AlgorithmResults result = new AlgorithmResults(algorithmName);
        result.cloudletsProcessed = cloudletsProcessed;
        result.avgExecutionTime = avgExecutionTime;
        result.slowestTime = slowestTime;
        result.fastestTime = fastestTime;
        result.efficiency = efficiency;
        result.vmDistribution = new HashMap<>(vmDistribution);
        
        // Calculate additional metrics
        // Throughput: cloudlets per hour (handle very large execution times)
        if (cloudletsProcessed > 0 && avgExecutionTime > 0) {
            // Convert execution time from seconds to hours for throughput calculation
            double executionTimeHours = avgExecutionTime / 3600.0;
            result.throughput = cloudletsProcessed / executionTimeHours;
            
            // If throughput is still too small, calculate as cloudlets per day instead
            if (result.throughput < 0.01) {
                double executionTimeDays = avgExecutionTime / 86400.0;
                result.throughput = cloudletsProcessed / executionTimeDays; // cloudlets per day
            }
        } else {
            result.throughput = 0;
        }
        result.responseTime = avgExecutionTime; // average response time
        result.peakConcurrentTransactions = peakConcurrentTransactions;
        
        results.add(result);
        history.add(result);
        System.out.println("Added results for " + algorithmName + ": " + cloudletsProcessed + " cloudlets processed");
    }
    
    public static void generateDynamicCharts() {
        if (results.isEmpty()) {
            System.out.println("No results available for chart generation.");
            return;
        }
        
        try {
            // Create comprehensive comparison chart
            BufferedImage comprehensiveChart = createComprehensiveChart();
            ImageIO.write(comprehensiveChart, "PNG", new File("dynamic_load_balancing_performance.png"));
            System.out.println("Dynamic chart saved as: dynamic_load_balancing_performance.png");
            
            // Create individual detailed charts
            createDetailedCloudletsChart();
            createDetailedExecutionTimeChart();
            createDetailedEfficiencyChart();
            createThroughputChart();
            createLoadDistributionChart();
            
            // Print analysis
            printDynamicAnalysis();
            
        } catch (IOException e) {
            System.err.println("Error creating dynamic charts: " + e.getMessage());
        }
    }
    
    private static BufferedImage createComprehensiveChart() {
        int width = 1400;
        int height = 1000;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // Set background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        
        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Title
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        String title = "Dynamic Load Balancing Algorithms Performance Comparison";
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g2d.drawString(title, (width - titleWidth) / 2, 40);
        
        // Chart 1: Cloudlets Processed
        double[] cloudletsData = results.stream().mapToDouble(r -> r.cloudletsProcessed).toArray();
        drawEnhancedBarChart(g2d, 50, 80, 350, 250, "Cloudlets Processed", cloudletsData, 
                           getAlgorithmColors(), "Cloudlets");
        
        // Chart 2: Average Execution Time (seconds)
        double[] executionData = results.stream().mapToDouble(r -> r.avgExecutionTime).toArray();
        drawEnhancedBarChart(g2d, 450, 80, 350, 250, "Average Execution Time (seconds)", executionData,
                           getAlgorithmColors(), "Seconds");
        
        // Chart 3: Load Balancing Efficiency
        double[] efficiencyData = results.stream().mapToDouble(r -> r.efficiency).toArray();
        drawEnhancedBarChart(g2d, 850, 80, 350, 250, "Load Balancing Efficiency (%)", efficiencyData,
                           getAlgorithmColors(), "%");
        
        // Chart 4: Throughput
        double[] throughputData = results.stream().mapToDouble(r -> r.throughput).toArray();
        String throughputUnit = throughputData.length > 0 && throughputData[0] < 1.0 ? "Cloudlets/day" : "Cloudlets/hr";
        String throughputTitle = throughputData.length > 0 && throughputData[0] < 1.0 ? "Throughput (cloudlets/day)" : "Throughput (cloudlets/hour)";
        drawEnhancedBarChart(g2d, 50, 380, 350, 250, throughputTitle, throughputData,
                           getAlgorithmColors(), throughputUnit);
        
        // Chart 5: Execution Time Range
        drawExecutionTimeRangeChart(g2d, 450, 380, 350, 250);
        
        // Chart 6: Load Distribution Comparison
        drawLoadDistributionChart(g2d, 850, 380, 350, 250);
        
        // Legend
        drawEnhancedLegend(g2d, 50, 680);
        
        // Summary table
        drawSummaryTable(g2d, 450, 680);
        
        g2d.dispose();
        return image;
    }
    
    private static Color[] getAlgorithmColors() {
        Color[] colors = new Color[results.size()];
        Color[] baseColors = {
            new Color(46, 139, 87),   // Forest Green for ABC
            new Color(70, 130, 180),  // Steel Blue for WRR
            new Color(255, 140, 0)    // Dark Orange for LC
        };
        
        for (int i = 0; i < colors.length && i < baseColors.length; i++) {
            colors[i] = baseColors[i];
        }
        
        return colors;
    }
    
    private static void drawEnhancedBarChart(Graphics2D g2d, int x, int y, int width, int height, 
                                           String title, double[] values, Color[] colors, String unit) {
        // Chart background
        g2d.setColor(new Color(240, 240, 240));
        g2d.fillRect(x, y, width, height);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, width, height);
        
        // Title
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g2d.drawString(title, x + (width - titleWidth) / 2, y - 10);
        
        // Find max value for scaling
        double maxValue = Arrays.stream(values).max().orElse(1.0);
        if (maxValue == 0) maxValue = 1.0;
        
        // Draw bars
        int barWidth = Math.max(40, width / values.length - 15);
        int startX = x + 20;
        
        for (int i = 0; i < values.length; i++) {
            int barHeight = (int) ((values[i] / maxValue) * (height - 80));
            int barX = startX + i * (barWidth + 15);
            int barY = y + height - 60 - barHeight;
            
            // Draw bar with gradient
            GradientPaint gradient = new GradientPaint(barX, barY, colors[i], 
                                                     barX, barY + barHeight, colors[i].darker());
            g2d.setPaint(gradient);
            g2d.fillRect(barX, barY, barWidth, barHeight);
            
            // Draw border
            g2d.setColor(Color.BLACK);
            g2d.drawRect(barX, barY, barWidth, barHeight);
            
            // Draw value label
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            String label = String.format("%.1f", values[i]);
            fm = g2d.getFontMetrics();
            int labelWidth = fm.stringWidth(label);
            g2d.drawString(label, barX + (barWidth - labelWidth) / 2, barY - 5);
            
            // Draw algorithm name
            g2d.setFont(new Font("Arial", Font.PLAIN, 11));
            fm = g2d.getFontMetrics();
            String algName = results.get(i).name;
            g2d.drawString(algName, barX + (barWidth - fm.stringWidth(algName)) / 2, y + height - 20);
        }
    }
    
    private static void drawExecutionTimeRangeChart(Graphics2D g2d, int x, int y, int width, int height) {
        // Chart background
        g2d.setColor(new Color(240, 240, 240));
        g2d.fillRect(x, y, width, height);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, width, height);
        
        // Title
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2d.getFontMetrics();
        String title = "Execution Time Range";
        int titleWidth = fm.stringWidth(title);
        g2d.drawString(title, x + (width - titleWidth) / 2, y - 10);
        
        // Find max value for scaling
        double maxValue = 0;
        for (AlgorithmResults result : results) {
            maxValue = Math.max(maxValue, result.slowestTime);
        }
        if (maxValue == 0) maxValue = 1.0;
        
        // Draw range bars
        int barWidth = Math.max(40, width / results.size() - 15);
        int startX = x + 20;
        
        for (int i = 0; i < results.size(); i++) {
            AlgorithmResults result = results.get(i);
            int barX = startX + i * (barWidth + 15);
            
            // Fastest time (green)
            int fastestHeight = (int) ((result.fastestTime / maxValue) * (height - 80));
            int fastestY = y + height - 60 - fastestHeight;
            g2d.setColor(new Color(0, 150, 0));
            g2d.fillRect(barX, fastestY, barWidth, fastestHeight);
            
            // Additional time (orange)
            int additionalHeight = (int) (((result.slowestTime - result.fastestTime) / maxValue) * (height - 80));
            int additionalY = fastestY - additionalHeight;
            g2d.setColor(new Color(255, 140, 0));
            g2d.fillRect(barX, additionalY, barWidth, additionalHeight);
            
            // Draw border
            g2d.setColor(Color.BLACK);
            g2d.drawRect(barX, additionalY, barWidth, fastestHeight + additionalHeight);
            
            // Draw algorithm name
            g2d.setFont(new Font("Arial", Font.PLAIN, 11));
            fm = g2d.getFontMetrics();
            g2d.drawString(result.name, barX + (barWidth - fm.stringWidth(result.name)) / 2, y + height - 20);
        }
    }
    
    private static void drawLoadDistributionChart(Graphics2D g2d, int x, int y, int width, int height) {
        // Chart background
        g2d.setColor(new Color(240, 240, 240));
        g2d.fillRect(x, y, width, height);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, width, height);
        
        // Title
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2d.getFontMetrics();
        String title = "Load Distribution Variance";
        int titleWidth = fm.stringWidth(title);
        g2d.drawString(title, x + (width - titleWidth) / 2, y - 10);
        
        // Calculate variance for each algorithm
        double[] variances = new double[results.size()];
        for (int i = 0; i < results.size(); i++) {
            AlgorithmResults result = results.get(i);
            if (result.vmDistribution.isEmpty()) {
                variances[i] = 0;
                continue;
            }
            
            double mean = result.vmDistribution.values().stream().mapToInt(Integer::intValue).average().orElse(0);
            double variance = result.vmDistribution.values().stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average().orElse(0);
            variances[i] = Math.sqrt(variance); // Standard deviation
        }
        
        // Draw variance bars
        double maxVariance = Arrays.stream(variances).max().orElse(1.0);
        if (maxVariance == 0) maxVariance = 1.0;
        
        int barWidth = Math.max(40, width / variances.length - 15);
        int startX = x + 20;
        
        for (int i = 0; i < variances.length; i++) {
            int barHeight = (int) ((variances[i] / maxVariance) * (height - 80));
            int barX = startX + i * (barWidth + 15);
            int barY = y + height - 60 - barHeight;
            
            // Color based on variance (lower is better)
            Color color = variances[i] < maxVariance * 0.3 ? new Color(0, 150, 0) :
                         variances[i] < maxVariance * 0.6 ? new Color(255, 140, 0) : Color.RED;
            
            g2d.setColor(color);
            g2d.fillRect(barX, barY, barWidth, barHeight);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(barX, barY, barWidth, barHeight);
            
            // Draw value
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            String label = String.format("%.2f", variances[i]);
            fm = g2d.getFontMetrics();
            int labelWidth = fm.stringWidth(label);
            g2d.drawString(label, barX + (barWidth - labelWidth) / 2, barY - 5);
            
            // Draw algorithm name
            g2d.setFont(new Font("Arial", Font.PLAIN, 11));
            fm = g2d.getFontMetrics();
            g2d.drawString(results.get(i).name, barX + (barWidth - fm.stringWidth(results.get(i).name)) / 2, y + height - 20);
        }
    }
    
    private static void drawEnhancedLegend(Graphics2D g2d, int x, int y) {
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(Color.BLACK);
        g2d.drawString("Legend:", x, y);
        
        y += 30;
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        
        // Algorithm colors
        Color[] colors = getAlgorithmColors();
        for (int i = 0; i < results.size() && i < colors.length; i++) {
            g2d.setColor(colors[i]);
            g2d.fillRect(x, y - 12, 20, 20);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(x, y - 12, 20, 20);
            g2d.drawString(results.get(i).name, x + 25, y);
            y += 25;
        }
        
        y += 10;
        // Additional legend items
        g2d.setColor(new Color(0, 150, 0));
        g2d.fillRect(x, y - 12, 20, 20);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y - 12, 20, 20);
        g2d.drawString("Fastest Execution Time", x + 25, y);
        
        y += 25;
        g2d.setColor(new Color(255, 140, 0));
        g2d.fillRect(x, y - 12, 20, 20);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y - 12, 20, 20);
        g2d.drawString("Additional Execution Time", x + 25, y);
    }
    
    private static void drawSummaryTable(Graphics2D g2d, int x, int y) {
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(Color.BLACK);
        g2d.drawString("Performance Summary:", x, y);
        
        y += 30;
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        
        // Table header
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("Algorithm", x, y);
        g2d.drawString("Cloudlets", x + 120, y);
        g2d.drawString("Avg Time(s)", x + 200, y);
        g2d.drawString("Efficiency%", x + 280, y);
        g2d.drawString("Throughput", x + 360, y);
        
        y += 20;
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        
        // Table data
        for (AlgorithmResults result : results) {
            g2d.drawString(result.name, x, y);
            g2d.drawString(String.valueOf(result.cloudletsProcessed), x + 120, y);
            g2d.drawString(String.format("%.2f", result.avgExecutionTime), x + 200, y);
            g2d.drawString(String.format("%.2f", result.efficiency), x + 280, y);
            String throughputText = result.throughput < 1.0 ? 
                String.format("%.6f", result.throughput) : 
                String.format("%.2f", result.throughput);
            g2d.drawString(throughputText, x + 360, y);
            y += 18;
        }
    }
    
    private static void createDetailedCloudletsChart() {
        try {
            BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, 800, 600);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            double[] cloudletsData = results.stream().mapToDouble(r -> r.cloudletsProcessed).toArray();
            drawEnhancedBarChart(g2d, 50, 50, 700, 500, "Cloudlets Processed Successfully", 
                               cloudletsData, getAlgorithmColors(), "Cloudlets");
            
            g2d.dispose();
            ImageIO.write(image, "PNG", new File("dynamic_cloudlets_processed.png"));
            System.out.println("Dynamic cloudlets chart saved as: dynamic_cloudlets_processed.png");
        } catch (IOException e) {
            System.err.println("Error creating dynamic cloudlets chart: " + e.getMessage());
        }
    }
    
    private static void createDetailedExecutionTimeChart() {
        try {
            BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, 800, 600);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            double[] executionData = results.stream().mapToDouble(r -> r.avgExecutionTime).toArray();
            drawEnhancedBarChart(g2d, 50, 50, 700, 500, "Average Execution Time (seconds)", 
                               executionData, getAlgorithmColors(), "Seconds");
            
            g2d.dispose();
            ImageIO.write(image, "PNG", new File("dynamic_execution_time.png"));
            System.out.println("Dynamic execution time chart saved as: dynamic_execution_time.png");
        } catch (IOException e) {
            System.err.println("Error creating dynamic execution time chart: " + e.getMessage());
        }
    }
    
    private static void createDetailedEfficiencyChart() {
        try {
            BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, 800, 600);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            double[] efficiencyData = results.stream().mapToDouble(r -> r.efficiency).toArray();
            drawEnhancedBarChart(g2d, 50, 50, 700, 500, "Load Balancing Efficiency (%)", 
                               efficiencyData, getAlgorithmColors(), "%");
            
            g2d.dispose();
            ImageIO.write(image, "PNG", new File("dynamic_efficiency.png"));
            System.out.println("Dynamic efficiency chart saved as: dynamic_efficiency.png");
        } catch (IOException e) {
            System.err.println("Error creating dynamic efficiency chart: " + e.getMessage());
        }
    }
    
    private static void createThroughputChart() {
        try {
            BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, 800, 600);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            double[] throughputData = results.stream().mapToDouble(r -> r.throughput).toArray();
            String throughputUnit = throughputData.length > 0 && throughputData[0] < 1.0 ? "Cloudlets/day" : "Cloudlets/hr";
            String throughputTitle = throughputData.length > 0 && throughputData[0] < 1.0 ? "Throughput (cloudlets per day)" : "Throughput (cloudlets per hour)";
            drawEnhancedBarChart(g2d, 50, 50, 700, 500, throughputTitle, 
                               throughputData, getAlgorithmColors(), throughputUnit);
            
            g2d.dispose();
            ImageIO.write(image, "PNG", new File("dynamic_throughput.png"));
            System.out.println("Dynamic throughput chart saved as: dynamic_throughput.png");
        } catch (IOException e) {
            System.err.println("Error creating dynamic throughput chart: " + e.getMessage());
        }
    }
    
    private static void createLoadDistributionChart() {
        try {
            BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, 800, 600);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            drawLoadDistributionChart(g2d, 50, 50, 700, 500);
            
            g2d.dispose();
            ImageIO.write(image, "PNG", new File("dynamic_load_distribution.png"));
            System.out.println("Dynamic load distribution chart saved as: dynamic_load_distribution.png");
        } catch (IOException e) {
            System.err.println("Error creating dynamic load distribution chart: " + e.getMessage());
        }
    }
    
    private static void printDynamicAnalysis() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("DYNAMIC LOAD BALANCING ALGORITHMS PERFORMANCE ANALYSIS");
        System.out.println("=".repeat(80));
        
        // Sort results by cloudlets processed (descending)
        results.sort((a, b) -> Integer.compare(b.cloudletsProcessed, a.cloudletsProcessed));
        
        System.out.println("\nRANKING BY PERFORMANCE:");
        System.out.println("-".repeat(60));
        System.out.printf("%-25s %-12s %-15s %-12s %s%n", "Algorithm", "Cloudlets", "Avg Time(s)", "Efficiency%", "Throughput");
        System.out.println("-".repeat(60));
        
        for (int i = 0; i < results.size(); i++) {
            AlgorithmResults result = results.get(i);
            String throughputText = result.throughput < 1.0 ? 
                String.format("%.6f", result.throughput) : 
                String.format("%.2f", result.throughput);
            System.out.printf("%-25s %-12d %-15.2f %-12.2f %s%n", 
                result.name, result.cloudletsProcessed, result.avgExecutionTime, 
                result.efficiency, throughputText);
        }
        
        System.out.println("\nDETAILED ANALYSIS:");
        System.out.println("-".repeat(60));
        
        for (AlgorithmResults result : results) {
            System.out.println("\n" + result.name + ":");
            System.out.println("  • Cloudlets Processed: " + result.cloudletsProcessed);
            System.out.println("  • Average Execution Time: " + String.format("%.2f", result.avgExecutionTime) + " seconds");
            System.out.println("  • Execution Time Range: " + String.format("%.2f", result.fastestTime) + " - " + 
                             String.format("%.2f", result.slowestTime) + " seconds");
            System.out.println("  • Load Balancing Efficiency: " + String.format("%.2f", result.efficiency) + "%");
            String throughputUnit = result.throughput < 1.0 ? "cloudlets/day" : "cloudlets/hour";
            System.out.println("  • Throughput: " + String.format("%.6f", result.throughput) + " " + throughputUnit);
            System.out.println("  • Load Distribution: " + result.vmDistribution);
        }
        
        // Find best performing algorithm
        if (!results.isEmpty()) {
            AlgorithmResults bestOverall = results.stream()
                .max(Comparator.comparing((AlgorithmResults r) -> r.cloudletsProcessed * (100 - r.efficiency) / Math.max(r.avgExecutionTime, 1)))
                .orElse(results.get(0));
            
            System.out.println("\nBEST OVERALL PERFORMER: " + bestOverall.name);
            System.out.println("-".repeat(40));
            System.out.println("• Highest cloudlet processing rate");
            System.out.println("• Optimal load balancing efficiency");
            System.out.println("• Best throughput-to-efficiency ratio");
        }
    }
    
    public static void clearResults() {
        results.clear();
    }
    
    public static List<AlgorithmResults> getHistory() {
        return new ArrayList<>(history);
    }
    
    public static List<AlgorithmResults> getResults() {
        return new ArrayList<>(results);
    }
}

// New chart to plot Avg Time vs Number of Cloudlets for all algorithms in the same graph
// Uses results accumulated in 'history' during the current application run
class AvgTimeVsCloudletsChart {
    private final List<DynamicPerformanceAnalyzer.AlgorithmResults> data;

    AvgTimeVsCloudletsChart(List<DynamicPerformanceAnalyzer.AlgorithmResults> data) {
        this.data = data;
    }

    public void save(String outputPath) throws IOException {
        // Prepare per-algorithm series: cloudletsProcessed -> avgExecutionTime
        Map<String, Map<Integer, Double>> seriesMap = new HashMap<>();
        for (DynamicPerformanceAnalyzer.AlgorithmResults r : data) {
            seriesMap.computeIfAbsent(r.name, _ -> new HashMap<>())
                    .put(r.cloudletsProcessed, r.avgExecutionTime);
        }

        // Define canvas
        int width = 900;
        int height = 600;
        int marginLeft = 80;
        int marginRight = 30;
        int marginTop = 50;
        int marginBottom = 70;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Determine axis ranges
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        for (Map<Integer, Double> s : seriesMap.values()) {
            for (Map.Entry<Integer, Double> e : s.entrySet()) {
                minX = Math.min(minX, e.getKey());
                maxX = Math.max(maxX, e.getKey());
                minY = Math.min(minY, e.getValue());
                maxY = Math.max(maxY, e.getValue());
            }
        }
        if (minX == Integer.MAX_VALUE) {
            minX = 0; maxX = 1; minY = 0; maxY = 1;
        }

        // Add some padding to Y range
        double yPadding = (maxY - minY) * 0.1;
        if (yPadding == 0) yPadding = 1.0;
        minY -= yPadding;
        maxY += yPadding;

        // Axes
        int plotX0 = marginLeft;
        int plotY0 = height - marginBottom;
        int plotX1 = width - marginRight;
        int plotY1 = marginTop;

        g.setColor(Color.BLACK);
        g.drawLine(plotX0, plotY0, plotX1, plotY0); // X axis
        g.drawLine(plotX0, plotY0, plotX0, plotY1); // Y axis

        // Titles
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        String title = "Average Time vs Number of Cloudlets";
        int titleW = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (width - titleW) / 2, marginTop - 15);

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("Number of Cloudlets", (plotX0 + plotX1) / 2 - 60, height - 35);
        g.rotate(-Math.PI / 2);
        g.drawString("Average Time (s)", -(plotY0 + plotY1) / 2 - 40, 25);
        g.rotate(Math.PI / 2);

        // Ticks/grid
        g.setColor(new Color(230, 230, 230));
        int xTicks = Math.max(2, Math.min(10, (maxX - minX)));
        int yTicks = 8;
        for (int i = 0; i <= xTicks; i++) {
            int x = plotX0 + (plotX1 - plotX0) * i / xTicks;
            g.drawLine(x, plotY0, x, plotY1);
        }
        for (int i = 0; i <= yTicks; i++) {
            int y = plotY0 - (plotY0 - plotY1) * i / yTicks;
            g.drawLine(plotX0, y, plotX1, y);
        }

        // Axis labels
        g.setColor(Color.DARK_GRAY);
        for (int i = 0; i <= xTicks; i++) {
            int xPix = plotX0 + (plotX1 - plotX0) * i / xTicks;
            int xVal = minX + (maxX - minX) * i / xTicks;
            String s = String.valueOf(xVal);
            int sw = g.getFontMetrics().stringWidth(s);
            g.drawString(s, xPix - sw / 2, plotY0 + 20);
        }
        for (int i = 0; i <= yTicks; i++) {
            int yPix = plotY0 - (plotY0 - plotY1) * i / yTicks;
            double yVal = minY + (maxY - minY) * i / yTicks;
            String s = String.format("%.2f", yVal);
            int sw = g.getFontMetrics().stringWidth(s);
            g.drawString(s, plotX0 - sw - 10, yPix + 4);
        }

        // Colors per series
        Color[] palette = new Color[]{
                new Color(52, 152, 219), // blue
                new Color(46, 204, 113), // green
                new Color(231, 76, 60),  // red
                new Color(155, 89, 182), // purple
                new Color(241, 196, 15)  // yellow
        };

        // Plot lines
        int colorIdx = 0;
        Map<String, Color> legendColors = new HashMap<>();
        for (Map.Entry<String, Map<Integer, Double>> entry : seriesMap.entrySet()) {
            String algo = entry.getKey();
            Map<Integer, Double> points = entry.getValue();
            List<Integer> xs = new ArrayList<>(points.keySet());
            xs.sort(Integer::compareTo);

            Color color = palette[colorIdx % palette.length];
            colorIdx++;
            legendColors.put(algo, color);
            g.setColor(color);

            int prevX = -1, prevY = -1;
            for (Integer xVal : xs) {
                double yVal = points.get(xVal);
                int xPix = plotX0 + (int)((xVal - minX) * 1.0 / Math.max(1, (maxX - minX)) * (plotX1 - plotX0));
                int yPix = plotY0 - (int)((yVal - minY) / Math.max(1e-9, (maxY - minY)) * (plotY0 - plotY1));
                g.fillOval(xPix - 3, yPix - 3, 6, 6);
                if (prevX >= 0) {
                    g.drawLine(prevX, prevY, xPix, yPix);
                }
                prevX = xPix; prevY = yPix;
            }
        }

        // Legend
        int legendX = plotX0 + 20;
        int legendY = plotY1 + 20;
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(Color.BLACK);
        g.drawString("Legend:", legendX, legendY);
        int offset = 15;
        int i = 1;
        for (Map.Entry<String, Color> e : legendColors.entrySet()) {
            int y = legendY + i * offset;
            g.setColor(e.getValue());
            g.fillRect(legendX, y - 10, 12, 12);
            g.setColor(Color.BLACK);
            g.drawString(e.getKey(), legendX + 18, y);
            i++;
        }

        g.dispose();
        ImageIO.write(image, "png", new File(outputPath));
    }
}

// Public facade to trigger chart creation without changing existing logic
class AvgTimeVsCloudletsFacade {
    public static void plot(List<DynamicPerformanceAnalyzer.AlgorithmResults> history, String outputPath) {
        try {
            new AvgTimeVsCloudletsChart(history).save(outputPath);
            System.out.println("Saved chart: " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to save chart: " + e.getMessage());
        }
    }
}

// New chart to plot Peak Concurrent Transactions vs Number of Cloudlets
class ConcurrentVsCloudletsChart {
    private final List<DynamicPerformanceAnalyzer.AlgorithmResults> data;

    ConcurrentVsCloudletsChart(List<DynamicPerformanceAnalyzer.AlgorithmResults> data) {
        this.data = data;
    }

    public void save(String outputPath) throws IOException {
        Map<String, Map<Integer, Integer>> seriesMap = new HashMap<>();
        for (DynamicPerformanceAnalyzer.AlgorithmResults r : data) {
            // ensure we keep the highest peak seen for a given cloudlet count
            Map<Integer, Integer> m = seriesMap.computeIfAbsent(r.name, k -> new HashMap<>());
            m.put(r.cloudletsProcessed, Math.max(m.getOrDefault(r.cloudletsProcessed, 0), r.peakConcurrentTransactions));
        }

        int width = 900;
        int height = 600;
        int marginLeft = 80, marginRight = 30, marginTop = 50, marginBottom = 70;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (Map<Integer, Integer> s : seriesMap.values()) {
            for (Map.Entry<Integer, Integer> e : s.entrySet()) {
                minX = Math.min(minX, e.getKey());
                maxX = Math.max(maxX, e.getKey());
                minY = Math.min(minY, e.getValue());
                maxY = Math.max(maxY, e.getValue());
            }
        }
        if (minX == Integer.MAX_VALUE) { minX = 0; maxX = 1; minY = 0; maxY = 1; }
        int yPad = Math.max(1, (int)((maxY - minY) * 0.1));
        minY -= yPad; maxY += yPad;

        int plotX0 = marginLeft, plotY0 = height - marginBottom, plotX1 = width - marginRight, plotY1 = marginTop;
        g.setColor(Color.BLACK);
        g.drawLine(plotX0, plotY0, plotX1, plotY0);
        g.drawLine(plotX0, plotY0, plotX0, plotY1);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        String title = "Peak Concurrent Transactions vs Number of Cloudlets";
        int titleW = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (width - titleW) / 2, marginTop - 15);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("Number of Cloudlets", (plotX0 + plotX1) / 2 - 60, height - 35);
        g.rotate(-Math.PI / 2);
        g.drawString("Peak Concurrent Transactions", -(plotY0 + plotY1) / 2 - 60, 25);
        g.rotate(Math.PI / 2);

        g.setColor(new Color(230, 230, 230));
        int xTicks = Math.max(2, Math.min(10, (maxX - minX)));
        int yTicks = 8;
        for (int i = 0; i <= xTicks; i++) {
            int x = plotX0 + (plotX1 - plotX0) * i / xTicks;
            g.drawLine(x, plotY0, x, plotY1);
        }
        for (int i = 0; i <= yTicks; i++) {
            int y = plotY0 - (plotY0 - plotY1) * i / yTicks;
            g.drawLine(plotX0, y, plotX1, y);
        }

        g.setColor(Color.DARK_GRAY);
        for (int i = 0; i <= xTicks; i++) {
            int xPix = plotX0 + (plotX1 - plotX0) * i / xTicks;
            int xVal = minX + (maxX - minX) * i / xTicks;
            String s = String.valueOf(xVal);
            int sw = g.getFontMetrics().stringWidth(s);
            g.drawString(s, xPix - sw / 2, plotY0 + 20);
        }
        for (int i = 0; i <= yTicks; i++) {
            int yPix = plotY0 - (plotY0 - plotY1) * i / yTicks;
            int yVal = minY + (maxY - minY) * i / yTicks;
            String s = String.valueOf(yVal);
            int sw = g.getFontMetrics().stringWidth(s);
            g.drawString(s, plotX0 - sw - 10, yPix + 4);
        }

        Color[] palette = new Color[]{ new Color(52,152,219), new Color(46,204,113), new Color(231,76,60), new Color(155,89,182) };
        int colorIdx = 0;
        Map<String, Color> legendColors = new HashMap<>();
        for (Map.Entry<String, Map<Integer, Integer>> entry : seriesMap.entrySet()) {
            String algo = entry.getKey();
            Map<Integer, Integer> points = entry.getValue();
            java.util.List<Integer> xs = new java.util.ArrayList<>(points.keySet());
            xs.sort(Integer::compareTo);
            Color color = palette[colorIdx % palette.length];
            colorIdx++;
            legendColors.put(algo, color);
            g.setColor(color);
            int prevX = -1, prevY = -1;
            for (Integer xVal : xs) {
                int yVal = points.get(xVal);
                int xPix = plotX0 + (int)((xVal - minX) * 1.0 / Math.max(1, (maxX - minX)) * (plotX1 - plotX0));
                int yPix = plotY0 - (int)((yVal - minY) / Math.max(1, (maxY - minY)) * (plotY0 - plotY1));
                g.fillOval(xPix - 3, yPix - 3, 6, 6);
                if (prevX >= 0) g.drawLine(prevX, prevY, xPix, yPix);
                prevX = xPix; prevY = yPix;
            }
        }

        int legendX = plotX0 + 20, legendY = plotY1 + 20;
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(Color.BLACK);
        g.drawString("Legend:", legendX, legendY);
        int offset = 15; int i = 1;
        for (Map.Entry<String, Color> e : new java.util.ArrayList<>(legendColors.entrySet())) {
            int y = legendY + i * offset;
            g.setColor(e.getValue());
            g.fillRect(legendX, y - 10, 12, 12);
            g.setColor(Color.BLACK);
            g.drawString(e.getKey(), legendX + 18, y);
            i++;
        }

        g.dispose();
        ImageIO.write(image, "png", new File(outputPath));
    }
}

class ConcurrentVsCloudletsFacade {
    public static void plot(List<DynamicPerformanceAnalyzer.AlgorithmResults> history, String outputPath) {
        try {
            new ConcurrentVsCloudletsChart(history).save(outputPath);
            System.out.println("Saved chart: " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to save chart: " + e.getMessage());
        }
    }
}

// New chart to plot Throughput vs Number of Cloudlets for all algorithms
class ThroughputVsCloudletsChart {
    private final List<DynamicPerformanceAnalyzer.AlgorithmResults> data;

    ThroughputVsCloudletsChart(List<DynamicPerformanceAnalyzer.AlgorithmResults> data) {
        this.data = data;
    }

    public void save(String outputPath) throws IOException {
        // Prepare per-algorithm series: cloudletsProcessed -> throughput
        Map<String, Map<Integer, Double>> seriesMap = new HashMap<>();
        for (DynamicPerformanceAnalyzer.AlgorithmResults r : data) {
            seriesMap.computeIfAbsent(r.name, _ -> new HashMap<>())
                    .put(r.cloudletsProcessed, r.throughput);
        }

        // Define canvas
        int width = 900;
        int height = 600;
        int marginLeft = 80;
        int marginRight = 30;
        int marginTop = 50;
        int marginBottom = 70;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Determine axis ranges
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        for (Map<Integer, Double> s : seriesMap.values()) {
            for (Map.Entry<Integer, Double> e : s.entrySet()) {
                minX = Math.min(minX, e.getKey());
                maxX = Math.max(maxX, e.getKey());
                minY = Math.min(minY, e.getValue());
                maxY = Math.max(maxY, e.getValue());
            }
        }
        if (minX == Integer.MAX_VALUE) {
            minX = 0; maxX = 1; minY = 0; maxY = 1;
        }

        // Add some padding to Y range
        double yPadding = (maxY - minY) * 0.1;
        if (yPadding == 0) yPadding = 1.0;
        minY -= yPadding;
        maxY += yPadding;

        // Axes
        int plotX0 = marginLeft;
        int plotY0 = height - marginBottom;
        int plotX1 = width - marginRight;
        int plotY1 = marginTop;

        g.setColor(Color.BLACK);
        g.drawLine(plotX0, plotY0, plotX1, plotY0); // X axis
        g.drawLine(plotX0, plotY0, plotX0, plotY1); // Y axis

        // Titles
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        String title = "Throughput vs Number of Cloudlets";
        int titleW = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (width - titleW) / 2, marginTop - 15);

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("Number of Cloudlets", (plotX0 + plotX1) / 2 - 60, height - 35);
        g.rotate(-Math.PI / 2);
        // Determine appropriate unit based on data
        String yAxisLabel = "Throughput (cloudlets/hour)";
        if (!seriesMap.isEmpty()) {
            double maxThroughput = seriesMap.values().stream()
                .flatMap(m -> m.values().stream())
                .mapToDouble(Double::doubleValue)
                .max().orElse(0);
            if (maxThroughput < 1.0) {
                yAxisLabel = "Throughput (cloudlets/day)";
            }
        }
        g.drawString(yAxisLabel, -(plotY0 + plotY1) / 2 - 60, 25);
        g.rotate(Math.PI / 2);

        // Ticks/grid
        g.setColor(new Color(230, 230, 230));
        int xTicks = Math.max(2, Math.min(10, (maxX - minX)));
        int yTicks = 8;
        for (int i = 0; i <= xTicks; i++) {
            int x = plotX0 + (plotX1 - plotX0) * i / xTicks;
            g.drawLine(x, plotY0, x, plotY1);
        }
        for (int i = 0; i <= yTicks; i++) {
            int y = plotY0 - (plotY0 - plotY1) * i / yTicks;
            g.drawLine(plotX0, y, plotX1, y);
        }

        // Axis labels
        g.setColor(Color.DARK_GRAY);
        for (int i = 0; i <= xTicks; i++) {
            int xPix = plotX0 + (plotX1 - plotX0) * i / xTicks;
            int xVal = minX + (maxX - minX) * i / xTicks;
            String s = String.valueOf(xVal);
            int sw = g.getFontMetrics().stringWidth(s);
            g.drawString(s, xPix - sw / 2, plotY0 + 20);
        }
        for (int i = 0; i <= yTicks; i++) {
            int yPix = plotY0 - (plotY0 - plotY1) * i / yTicks;
            double yVal = minY + (maxY - minY) * i / yTicks;
            String s = String.format("%.0f", yVal);
            int sw = g.getFontMetrics().stringWidth(s);
            g.drawString(s, plotX0 - sw - 10, yPix + 4);
        }

        // Colors per series
        Color[] palette = new Color[]{
                new Color(52, 152, 219), // blue
                new Color(46, 204, 113), // green
                new Color(231, 76, 60),  // red
                new Color(155, 89, 182), // purple
                new Color(241, 196, 15)  // yellow
        };

        // Plot lines
        int colorIdx = 0;
        Map<String, Color> legendColors = new HashMap<>();
        for (Map.Entry<String, Map<Integer, Double>> entry : seriesMap.entrySet()) {
            String algo = entry.getKey();
            Map<Integer, Double> points = entry.getValue();
            List<Integer> xs = new ArrayList<>(points.keySet());
            xs.sort(Integer::compareTo);

            Color color = palette[colorIdx % palette.length];
            colorIdx++;
            legendColors.put(algo, color);
            g.setColor(color);

            int prevX = -1, prevY = -1;
            for (Integer xVal : xs) {
                double yVal = points.get(xVal);
                int xPix = plotX0 + (int)((xVal - minX) * 1.0 / Math.max(1, (maxX - minX)) * (plotX1 - plotX0));
                int yPix = plotY0 - (int)((yVal - minY) / Math.max(1e-9, (maxY - minY)) * (plotY0 - plotY1));
                g.fillOval(xPix - 3, yPix - 3, 6, 6);
                if (prevX >= 0) {
                    g.drawLine(prevX, prevY, xPix, yPix);
                }
                prevX = xPix; prevY = yPix;
            }
        }

        // Legend
        int legendX = plotX0 + 20;
        int legendY = plotY1 + 20;
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(Color.BLACK);
        g.drawString("Legend:", legendX, legendY);
        int offset = 15;
        int i = 1;
        for (Map.Entry<String, Color> e : legendColors.entrySet()) {
            int y = legendY + i * offset;
            g.setColor(e.getValue());
            g.fillRect(legendX, y - 10, 12, 12);
            g.setColor(Color.BLACK);
            g.drawString(e.getKey(), legendX + 18, y);
            i++;
        }

        g.dispose();
        ImageIO.write(image, "png", new File(outputPath));
    }
}

// Public facade to trigger throughput chart creation
class ThroughputVsCloudletsFacade {
    public static void plot(List<DynamicPerformanceAnalyzer.AlgorithmResults> history, String outputPath) {
        try {
            new ThroughputVsCloudletsChart(history).save(outputPath);
            System.out.println("Saved throughput chart: " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to save throughput chart: " + e.getMessage());
        }
    }
}
