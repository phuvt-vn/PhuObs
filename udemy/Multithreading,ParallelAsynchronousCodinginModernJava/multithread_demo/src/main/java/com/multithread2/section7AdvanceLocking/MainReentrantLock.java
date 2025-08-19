package com.multithread2.section7AdvanceLocking;/*
 * Copyright (c) 2018-2023. Michael Pogrebinsky - Top Developer Academy
 * https://topdeveloperacademy.com
 * All rights reserved
 */

import javafx.animation.AnimationTimer;
import javafx.animation.FillTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Ví dụ về ReentrantLock trong ứng dụng giao diện người dùng (UI).
 * Chương trình này tạo một cửa sổ JavaFX hiển thị giá của các loại tiền điện tử.
 * Có hai luồng chính hoạt động:
 * 1. Luồng Giao diện người dùng (UI Thread) của JavaFX: Chịu trách nhiệm cập nhật các Label trên màn hình.
 * 2. Luồng Cập nhật giá (PriceUpdater): Chạy trong nền, liên tục cập nhật giá một cách ngẫu nhiên.
 *
 * Vấn đề cần giải quyết: Làm thế nào để hai luồng này truy cập và sửa đổi dữ liệu giá một cách an toàn?
 * Giải pháp: Sử dụng ReentrantLock để bảo vệ dữ liệu giá (trong PricesContainer).
 */

// Lệnh để chạy ứng dụng này từ terminal (yêu cầu đã cấu hình pom.xml cho JavaFX):
// mvn javafx:run

public class MainReentrantLock extends Application {
    public static void main(String[] args) {
        // Khởi chạy ứng dụng JavaFX.
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Cryptocurrency Prices");

        GridPane grid = createGrid();
        Map<String, Label> cryptoLabels = createCryptoPriceLabels();

        addLabelsToGrid(cryptoLabels, grid);

        double width = 300;
        double height = 250;

        StackPane root = new StackPane();

        Rectangle background = createBackgroundRectangleWithAnimation(width, height);

        root.getChildren().add(background);
        root.getChildren().add(grid);

        primaryStage.setScene(new Scene(root, width, height));

        // Tạo đối tượng chứa dữ liệu giá, sẽ được chia sẻ giữa các luồng.
        PricesContainer pricesContainer = new PricesContainer();

        // Tạo và khởi chạy luồng cập nhật giá trong nền.
        PriceUpdater priceUpdater = new PriceUpdater(pricesContainer);

        // AnimationTimer là một phần của JavaFX, nó chạy mã trong phương thức handle()
        // trên luồng UI ở mỗi khung hình (frame).
        AnimationTimer animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Sử dụng tryLock() thay vì lock() là một chiến lược quan trọng trong UI.
                // tryLock() sẽ không khóa luồng UI nếu lock đang được giữ bởi luồng khác.
                // Nó chỉ lấy lock nếu lock đang rảnh, nếu không nó sẽ bỏ qua và thử lại ở khung hình tiếp theo.
                // Điều này ngăn chặn việc đóng băng giao diện người dùng (UI freezing).
                if (pricesContainer.getLockObject().tryLock()) {
                    try {
                        // Nếu lấy được lock, an toàn đọc giá và cập nhật các Label.
                        Label bitcoinLabel = cryptoLabels.get("BTC");
                        bitcoinLabel.setText(String.valueOf(pricesContainer.getBitcoinPrice()));

                        Label etherLabel = cryptoLabels.get("ETH");
                        etherLabel.setText(String.valueOf(pricesContainer.getEtherPrice()));

                        Label litecoinLabel = cryptoLabels.get("LTC");
                        litecoinLabel.setText(String.valueOf(pricesContainer.getLitecoinPrice()));

                        Label bitcoinCashLabel = cryptoLabels.get("BCH");
                        bitcoinCashLabel.setText(String.valueOf(pricesContainer.getBitcoinCashPrice()));

                        Label rippleLabel = cryptoLabels.get("XRP");
                        rippleLabel.setText(String.valueOf(pricesContainer.getRipplePrice()));
                    } finally {
                        // Luôn đảm bảo giải phóng lock trong khối finally để tránh deadlock.
                        pricesContainer.getLockObject().unlock();
                    }
                }
            }
        };

        addWindowResizeListener(primaryStage, background);

        // Bắt đầu vòng lặp cập nhật UI.
        animationTimer.start();

        // Bắt đầu luồng cập nhật giá.
        priceUpdater.start();

        primaryStage.show();
    }

    // --- Các phương thức pomoc trợ để xây dựng giao diện JavaFX ---

    private void addWindowResizeListener(Stage stage, Rectangle background) {
        ChangeListener<Number> stageSizeListener = ((observable, oldValue, newValue) -> {
            background.setHeight(stage.getHeight());
            background.setWidth(stage.getWidth());
        });
        stage.widthProperty().addListener(stageSizeListener);
        stage.heightProperty().addListener(stageSizeListener);
    }

    private Map<String, Label> createCryptoPriceLabels() {
        Label bitcoinPrice = new Label("0");
        bitcoinPrice.setId("BTC");

        Label etherPrice = new Label("0");
        etherPrice.setId("ETH");

        Label liteCoinPrice = new Label("0");
        liteCoinPrice.setId("LTC");

        Label bitcoinCashPrice = new Label("0");
        bitcoinCashPrice.setId("BCH");

        Label ripplePrice = new Label("0");
        ripplePrice.setId("XRP");

        Map<String, Label> cryptoLabelsMap = new HashMap<>();
        cryptoLabelsMap.put("BTC", bitcoinPrice);
        cryptoLabelsMap.put("ETH", etherPrice);
        cryptoLabelsMap.put("LTC", liteCoinPrice);
        cryptoLabelsMap.put("BCH", bitcoinCashPrice);
        cryptoLabelsMap.put("XRP", ripplePrice);

        return cryptoLabelsMap;
    }

    private GridPane createGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);
        return grid;
    }

    private void addLabelsToGrid(Map<String, Label> labels, GridPane grid) {
        int row = 0;
        for (Map.Entry<String, Label> entry : labels.entrySet()) {
            String cryptoName = entry.getKey();
            Label nameLabel = new Label(cryptoName);
            nameLabel.setTextFill(Color.BLUE);
            nameLabel.setOnMousePressed(event -> nameLabel.setTextFill(Color.RED));
            nameLabel.setOnMouseReleased((EventHandler) event -> nameLabel.setTextFill(Color.BLUE));

            grid.add(nameLabel, 0, row);
            grid.add(entry.getValue(), 1, row);

            row++;
        }
    }

    private Rectangle createBackgroundRectangleWithAnimation(double width, double height) {
        Rectangle backround = new Rectangle(width, height);
        FillTransition fillTransition = new FillTransition(Duration.millis(1000), backround, Color.LIGHTGREEN, Color.LIGHTBLUE);
        fillTransition.setCycleCount(Timeline.INDEFINITE);
        fillTransition.setAutoReverse(true);
        fillTransition.play();
        return backround;
    }

    @Override
    public void stop() {
        // Đảm bảo ứng dụng thoát hoàn toàn khi cửa sổ bị đóng.
        System.exit(0);
    }

    /**
     * Lớp chứa dữ liệu giá được chia sẻ.
     * Nó chứa một đối tượng ReentrantLock để bảo vệ dữ liệu khỏi việc truy cập đồng thời không an toàn.
     */
    public static class PricesContainer {
        // ReentrantLock linh hoạt hơn so với khối synchronized.
        // Nó cho phép các chiến lược khóa phức tạp hơn như tryLock().
        private Lock lockObject = new ReentrantLock();

        private double bitcoinPrice;
        private double etherPrice;
        private double litecoinPrice;
        private double bitcoinCashPrice;
        private double ripplePrice;

        // Getter cho đối tượng lock để các luồng khác có thể sử dụng nó.
        public Lock getLockObject() {
            return lockObject;
        }

        public double getBitcoinPrice() {
            return bitcoinPrice;
        }

        public void setBitcoinPrice(double bitcoinPrice) {
            this.bitcoinPrice = bitcoinPrice;
        }

        public double getEtherPrice() {
            return etherPrice;
        }

        public void setEtherPrice(double etherPrice) {
            this.etherPrice = etherPrice;
        }

        public double getLitecoinPrice() {
            return litecoinPrice;
        }

        public void setLitecoinPrice(double litecoinPrice) {
            this.litecoinPrice = litecoinPrice;
        }

        public double getBitcoinCashPrice() {
            return bitcoinCashPrice;
        }

        public void setBitcoinCashPrice(double bitcoinCashPrice) {
            this.bitcoinCashPrice = bitcoinCashPrice;
        }

        public double getRipplePrice() {
            return ripplePrice;
        }

        public void setRipplePrice(double ripplePrice) {
            this.ripplePrice = ripplePrice;
        }
    }

    /**
     * Luồng này chạy trong nền và liên tục cập nhật giá tiền điện tử.
     */
    public static class PriceUpdater extends Thread {
        private PricesContainer pricesContainer;
        private Random random = new Random();

        public PriceUpdater(PricesContainer pricesContainer) {
            // Đảm bảo luồng này là luồng daemon. Luồng daemon sẽ tự động kết thúc
            // khi tất cả các luồng non-daemon (như luồng UI) đã kết thúc.
            this.setDaemon(true);
            this.pricesContainer = pricesContainer;
        }

        @Override
        public void run() {
            while (true) {
                // Luồng này sử dụng lock() vì nó phải đợi để có được quyền ghi giá mới.
                // Việc khóa luồng nền này trong một thời gian ngắn là chấp nhận được.
                pricesContainer.getLockObject().lock();
                try {
                    // Tạm dừng một chút để mô phỏng việc lấy giá từ một API.
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                    // Cập nhật tất cả các giá với giá trị ngẫu nhiên.
                    pricesContainer.setBitcoinPrice(random.nextInt(20000));
                    pricesContainer.setEtherPrice(random.nextInt(2000));
                    pricesContainer.setLitecoinPrice(random.nextInt(500));
                    pricesContainer.setBitcoinCashPrice(random.nextInt(5000));
                    pricesContainer.setRipplePrice(random.nextDouble());
                } finally {
                    // Luôn giải phóng lock sau khi hoàn thành việc ghi.
                    pricesContainer.getLockObject().unlock();
                }

                // Tạm dừng giữa các lần cập nhật.
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
