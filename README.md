# Short URL & QR Code Generator

A backend service that converts long URLs into trackable short links and customizable QR codes.

> 🚧 **Note:** This project is currently under construction.

## 💡 The Idea (The "Why")
While many URL shorteners exist, the core value of this service lies in **dynamic QR redirection**.

The generated QR code encodes the *shortlink*, not the final destination. This means you can change where a printed QR code points without ever needing to reprint it.

**Example:** You print table tents for a restaurant with a QR code pointing to the summer menu. Months later, you want to redirect customers to the winter menu. You simply update the target URL in the system, and all previously printed QR codes will seamlessly redirect to the new page.

## ⚙️ How It Works
1. A target URL (and optionally a custom image) is submitted to the service.
2. The system generates a unique shortlink and a QR code pointing specifically to that shortlink.
3. When a user clicks the link or scans the QR, their device makes an HTTP request to the shortlink endpoint.
4. The server intercepts this request, increments the access counter to track metrics, and redirects the user to the current target URL.

## 🛠️ Tech Stack
* **Java 21**
* **Spring Boot 4.1**
* **Spring Data JPA**
* **H2 Database** (file-based, local development)

## 🚀 How to Run

You can easily start the application from your terminal using the Maven wrapper.

1. Clone the repository and navigate to the project folder:
   ```bash
   git clone https://github.com/m4r10kyou/shortlinkQr
   cd shortlinkQr
   ```
2. Run the Spring Boot application:
    ```bash
    ./mvnw spring-boot:run
    ```
   *(If you are using Windows, run `mvnw.cmd spring-boot:run` instead).*


3. Once the application starts, it will be available at:
   ```text
   http://localhost:9080
   ```
4. Database Access:
   
   *You can inspect the local database using the H2 Console at:*
   ```text
   http://localhost:9080/h2-console 
   ```  

## 📋 Project Status
* [x] Initial project structure and dependencies setup.
* [ ] Core URL shortening and redirection logic.
* [ ] Access counter metric tracking.
* [ ] QR code generation and custom image embedding.