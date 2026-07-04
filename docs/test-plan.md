# Kế Hoạch Kiểm Thử & Báo Cáo Kết Quả (Test Plan & Report)

Dự án: **YAS (Yet Another Shop) - DevOps & Service Mesh Implementation**

---

## 1. Bảng Tổng Hợp Kết Quả Kiểm Thử (Test Execution Table)

### 1.1. Continuous Integration (CI)
*Hạng mục kiểm thử quy trình tự động hóa xây dựng và kiểm tra mã nguồn (GitHub Actions / Jenkins / GitLab CI).*

| ID | Test Case (Kịch bản kiểm thử) | Mục tiêu (Objective) | Lệnh chạy / Điều kiện kích hoạt | Kết quả kỳ vọng (Expected) | Kết quả thực tế (Actual) | Trạng thái (Status) | Link Ảnh / Log Bằng chứng (Evidence) |
|---|---|---|---|---|---|---|---|
| CI-01 | Tự động hóa build & test | Kích hoạt khi có commit mới lên branch chính để kiểm tra mã nguồn build thành công và vượt qua unit test. | Push code/Tạo Pull Request. | Toàn bộ pipeline chạy thành công (green build). | `[Chờ điền]` | `[Chờ điền]` | `[Chèn link ảnh/log tại đây]` |
| CI-02 | Tích hợp quét bảo mật mã nguồn | Quét kiểm tra bảo mật (ví dụ: SonarQube, Snyk hoặc Trivy) để phát hiện lỗ hổng. | Chạy tự động trong CI pipeline. | Không có lỗ hổng bảo mật nghiêm trọng (Critical/High). | `[Chờ điền]` | `[Chờ điền]` | `[Chèn link ảnh/log tại đây]` |

---

### 1.2. Docker Hub & Registry
*Hạng mục kiểm thử đóng gói và lưu trữ container image.*

| ID | Test Case (Kịch bản kiểm thử) | Mục tiêu (Objective) | Lệnh chạy / Điều kiện kích hoạt | Kết quả kỳ vọng (Expected) | Kết quả thực tế (Actual) | Trạng thái (Status) | Link Ảnh / Log Bằng chứng (Evidence) |
|---|---|---|---|---|---|---|---|
| DH-01 | Tự động push image lên Registry | Đóng gói mã nguồn thành Docker image và tải lên registry sau khi CI thành công. | Kích hoạt tự động cuối CI pipeline. | Image mới xuất hiện trên Docker Hub/Registry với tag tương ứng (ví dụ: `latest` hoặc commit SHA). | `[Chờ điền]` | `[Chờ điền]` | `[Chèn link ảnh/log tại đây]` |
| DH-02 | Quét bảo mật Docker Image | Quét image chống lỗ hổng bảo mật trong các thư viện hệ thống trước khi deploy. | Quét tự động trên Registry hoặc bằng Trivy trong pipeline. | Image sạch, báo cáo quét không có lỗ hổng nghiêm trọng. | `[Chờ điền]` | `[Chờ điền]` | `[Chèn link ảnh/log tại đây]` |

---

### 1.3. Kubernetes (K8s) Cluster Deployment
*Hạng mục kiểm thử trạng thái hoạt động của cụm tài nguyên trên Kubernetes.*

| ID | Test Case (Kịch bản kiểm thử) | Mục tiêu (Objective) | Lệnh chạy / Điều kiện kích hoạt | Kết quả kỳ vọng (Expected) | Kết quả thực tế (Actual) | Trạng thái (Status) | Link Ảnh / Log Bằng chứng (Evidence) |
|---|---|---|---|---|---|---|---|
| K8S-01| Trạng thái các Pod của ứng dụng | Xác nhận tất cả các Pod microservice đều khởi chạy thành công. | `kubectl get pods -n yas-dev` | Tất cả các Pod hiển thị trạng thái `Running` và các container đều `Ready` (ví dụ: `2/2` nếu đã cài đặt Sidecar). | `[Chờ điền]` | `[Chờ điền]` | `[Chèn link ảnh/log tại đây]` |
| K8S-02| Khả năng phân giải DNS nội bộ | Đảm bảo các service có thể tìm thấy và phân giải IP của nhau. | `nslookup product.yas-dev` chạy bên trong một pod bất kỳ. | Trả về đúng Cluster IP của service tương ứng. | `[Chờ điền]` | `[Chờ điền]` | `[Chèn link ảnh/log tại đây]` |

---

### 1.4. Developer Local Build (developer_build)
*Hạng mục kiểm thử khả năng chạy thử nghiệm ứng dụng ở môi trường local của lập trình viên.*

| ID | Test Case (Kịch bản kiểm thử) | Mục tiêu (Objective) | Lệnh chạy / Điều kiện kích hoạt | Kết quả kỳ vọng (Expected) | Kết quả thực tế (Actual) | Trạng thái (Status) | Link Ảnh / Log Bằng chứng (Evidence) |
|---|---|---|---|---|---|---|---|
| DEV-01| Build ứng dụng tại local | Đảm bảo lập trình viên có thể xây dựng ứng dụng độc lập trên máy cá nhân. | `mvn clean install` hoặc `./gradlew build` | Quá trình build thành công mà không gặp lỗi biên dịch. | `[Chờ điền]` | `[Chờ điền]` | `[Chèn link ảnh/log tại đây]` |
| DEV-02| Chạy ứng dụng qua Docker Compose | Khởi chạy nhanh toàn bộ stack dịch vụ phục vụ cho việc debug. | `docker-compose up -d` | Tất cả các container database, kafka, backend chạy lên thành công tại máy local. | `[Chờ điền]` | `[Chờ điền]` | `[Chèn link ảnh/log tại đây]` |

---

### 1.5. ArgoCD (GitOps)
*Hạng mục kiểm thử việc đồng bộ hóa cấu hình hệ thống bằng công cụ ArgoCD.*

| ID | Test Case (Kịch bản kiểm thử) | Mục tiêu (Objective) | Lệnh chạy / Điều kiện kích hoạt | Kết quả kỳ vọng (Expected) | Kết quả thực tế (Actual) | Trạng thái (Status) | Link Ảnh / Log Bằng chứng (Evidence) |
|---|---|---|---|---|---|---|---|
| ARG-01| Tự động đồng bộ (Auto Sync) | Khi có thay đổi về manifest trong Git repository của K8s, ArgoCD sẽ tự động đồng bộ xuống cụm. | Commit thay đổi số lượng replica hoặc cấu hình lên Git branch deploy. | Trạng thái ứng dụng trên ArgoCD chuyển sang `Synced` và K8s cập nhật tài nguyên mới. | `[Chờ điền]` | `[Chờ điền]` | `[Chèn link ảnh/log tại đây]` |
| ARG-02| Kiểm tra trạng thái đồng bộ | Đảm bảo tất cả ứng dụng định nghĩa trên ArgoCD khỏe mạnh. | Xem bảng điều khiển ArgoCD UI. | Trạng thái hiển thị là `Healthy` và `Synced`. | `[Chờ điền]` | `[Chờ điền]` | `[Chèn link ảnh/log tại đây]` |

---

### 1.6. Service Mesh (Istio)
*Hạng mục kiểm thử cấu hình dịch vụ, bảo mật đường truyền, phân quyền và khả năng chống chịu lỗi của Service Mesh.*

| ID | Test Case (Kịch bản kiểm thử) | Mục tiêu (Objective) | Lệnh chạy / Điều kiện kích hoạt | Kết quả kỳ vọng (Expected) | Kết quả thực tế (Actual) | Trạng thái (Status) | Link Ảnh / Log Bằng chứng (Evidence) |
|---|---|---|---|---|---|---|---|
| SM-01 | **Bật Istio Sidecar Injection** | Kích hoạt tự động nhúng proxy Envoy vào tất cả các Pod chạy trong namespace chỉ định. | `kubectl label namespace yas-dev istio-injection=enabled`<br>`kubectl rollout restart deployment -n yas-dev` | Toàn bộ Pod khi khởi chạy lại sẽ hiển thị số lượng container là `2/2` (1 container ứng dụng + 1 container `istio-proxy`). | Khớp hoàn toàn. Toàn bộ các Pod microservice trong namespace `yas-dev` đều có `READY 2/2` container và trạng thái `Running`. | **PASSED** | `[Chèn link ảnh kiểm tra trạng thái Pod]` |
| SM-02 | **Kích hoạt mTLS STRICT** | Mã hóa 100% dữ liệu trao đổi giữa các dịch vụ trong namespace và cấm các kết nối không mã hóa (Plaintext). | `kubectl apply -f istio/peer-authentication.yaml`<br><br>*Lệnh kiểm tra:*<br>`istioctl proxy-config secret <pod-name>.yas-dev` | Chứng chỉ `default` (Cert Chain) và `ROOTCA` đều ở trạng thái `ACTIVE` với `VALID CERT: true`. | Khớp hoàn toàn. Envoy proxy của Pod nhận thành công chứng chỉ và hiển thị `ACTIVE` / `VALID CERT: true`. | **PASSED** | `[Chèn link ảnh chạy lệnh istioctl proxy-config secret]` |
| SM-03 | **Phân quyền truy cập thành công (Allowed)** | Cho phép dịch vụ `storefront-bff` được quyền truy cập vào `product` và `cart`. | `kubectl apply -f istio/authorization-policy.yaml`<br><br>*Lệnh kiểm tra:*<br>`kubectl exec -n yas-dev deploy/storefront-bff -- wget -S --spider http://product.yas-dev:8090/actuator/health` | Kết nối thành công, máy chủ trả về mã trạng thái `HTTP/1.1 200 OK`. | Khớp hoàn toàn. Yêu cầu HTTP đi qua Envoy thành công và trả về mã trạng thái `HTTP/1.1 200 OK`. | **PASSED** | `[Chèn link ảnh test allowed từ storefront-bff]` |
| SM-04 | **Ngăn chặn truy cập trái phép (Denied)** | Các dịch vụ khác (ví dụ: `backoffice-bff`) gọi tới `product` và `cart` phải bị hệ thống bảo mật chặn lại. | `kubectl exec -n yas-dev deploy/backoffice-bff -- wget -S --spider http://product.yas-dev:8090/actuator/health` | Yêu cầu kết nối bị chặn ngay lập tức, trả về lỗi `HTTP/1.1 403 Forbidden` (hoặc thông báo lỗi access denied từ Envoy). | Khớp hoàn toàn. Envoy chặn cuộc gọi mạng và trả về mã lỗi `HTTP/1.1 403 Forbidden`. | **PASSED** | `[Chèn link ảnh test denied từ backoffice-bff]` |
| SM-05 | **Chính sách tự động gọi lại (Retry Policy)** | Khi dịch vụ `tax` gặp sự cố (lỗi 5xx), client proxy phải tự động thực hiện lại tối đa 3 lần. | `kubectl apply -f istio/virtual-service-retry.yaml`<br><br>*Kích hoạt kiểm thử bằng cách chạy EnvoyFilter giả lập lỗi 500 trên Pod tax và gửi request từ storefront-bff.* | Khi xem log của Pod `tax`, sẽ có đúng 4 cuộc gọi mạng liên tiếp (1 lần đầu + 3 lần retry) xuất hiện trong thời gian dưới 0.2 giây và chung 1 mã Request ID duy nhất. | Khớp hoàn toàn. Log sidecar của Pod `tax` ghi nhận 4 request `500 FI fault_filter_abort` giống hệt Request ID trong vòng 0.2 giây. | **PASSED** | `[Chèn link ảnh log 4 dòng của Pod tax]` |
| SM-06 | **Biểu đồ mạng Kiali (Kiali Topology)** | Quan sát trực quan luồng traffic thực tế trong cụm và trạng thái mã hóa bảo mật của các kết nối. | Mở Kiali Dashboard, chạy vòng lặp gửi request giữa các service, bật hiển thị badge "Security". | Sơ đồ mạng hiển thị rõ cấu trúc kết nối giữa các Pod, tất cả các đường kết nối đều có biểu tượng ổ khóa màu đen/trắng 🔒 biểu thị mTLS đang hoạt động. | Khớp hoàn toàn. Bản đồ topology của Kiali hiển thị đầy đủ sơ đồ các Pod đang hoạt động và biểu tượng ổ khóa mTLS trên các luồng kết nối. | **PASSED** | `[Chèn link ảnh chụp Kiali Topology có hình ổ khóa]` |
