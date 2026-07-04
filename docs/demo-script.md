# Kịch Bản Trình Bày Live Demo Service Mesh (Istio)

Tài liệu này cung cấp kịch bản từng bước (lệnh gõ trực tiếp) để trình bày trước giảng viên. Mỗi bước đều được đính kèm **dữ liệu log thực tế chạy thành công (backup)** phòng trường hợp gặp lỗi hoặc gián đoạn mạng trong lúc demo.

---

## BƯỚC 1: Bật Istio Sidecar Injection (Bước 4)
*Giới thiệu với giảng viên: "Trước tiên, em sẽ kích hoạt Istio sidecar injection trên namespace `yas-dev` để tự động nhúng Envoy Proxy vào các Pod của ứng dụng."*

### Lệnh gõ trực tiếp:
```bash
# 1. Kích hoạt tự động nhúng sidecar
kubectl label namespace yas-dev istio-injection=enabled

# 2. Restart các Deployment để áp dụng cấu hình
kubectl rollout restart deployment -n yas-dev

# 3. Kiểm tra các Pod xem đã có 2/2 container chưa
kubectl get pods -n yas-dev
```

### Phương án dự phòng (Backup Log):
Nếu Pod bị lỗi khởi động hoặc deploy chậm, giải thích và show log mẫu:
```text
NAME                                 READY   STATUS    RESTARTS   AGE
backoffice-bff-7ccc8b4bcb-76l6c      2/2     Running   0          6m
product-6545765f4c-xv2v6             2/2     Running   0          7m
storefront-bff-5594c594b8-8h6sq      2/2     Running   0          6m
```
*Nhấn mạnh: Cột `READY 2/2` chứng tỏ Envoy proxy (`istio-proxy`) đã chạy song song cùng ứng dụng.*

---

## BƯỚC 2: Kích hoạt mTLS STRICT & Kiểm tra (Bước 5)
*Giới thiệu: "Tiếp theo, em sẽ cấu hình bắt buộc mã hóa mTLS ở mức STRICT cho toàn bộ các giao tiếp nội bộ trong namespace `yas-dev`."*

### Lệnh gõ trực tiếp:
```bash
# 1. Áp dụng cấu hình PeerAuthentication
kubectl apply -f istio/peer-authentication.yaml

# 2. Lấy danh sách tên Pod hiện tại để thay thế vào lệnh dưới
kubectl get pods -n yas-dev

# 3. Kiểm tra cấu hình chứng chỉ bảo mật của Pod bất kỳ (ví dụ: tax)
istioctl proxy-config secret <pod-name-tax>.yas-dev
```

### Phương án dự phòng (Backup Log):
```text
RESOURCE NAME  TYPE        STATUS  VALID CERT  SERIAL NUMBER                      NOT AFTER             NOT BEFORE
default        Cert Chain  ACTIVE  true        0950091e4d3263d6a5c8a6e9fe13a938   2026-07-05T10:09:49Z  2026-07-04T10:07:49Z
ROOTCA         CA          ACTIVE  true        d9767f96997c371cd28ffacba0e4cb9    2036-07-01T10:04:02Z  2026-07-04T10:04:02Z
```
*Nhấn mạnh: Trạng thái `ACTIVE` và `VALID CERT: true` cho thấy chứng chỉ mTLS được tự động cấp phát và gia hạn thành công bởi Istiod.*

---

## BƯỚC 3: Cấu hình AuthorizationPolicy (Bước 6)
*Giới thiệu: "Em sẽ thiết lập chính sách phân quyền tối thiểu (Least Privilege). Chỉ cho phép ServiceAccount `storefront-bff` truy cập vào dịch vụ `product` và `cart`."*

### Lệnh gõ trực tiếp:
```bash
# 1. Áp dụng chính sách AuthorizationPolicy
kubectl apply -f istio/authorization-policy.yaml

# 2. Kiểm tra danh sách chính sách được tạo
kubectl get authorizationpolicy -n yas-dev
```

### Phương án dự phòng (Backup Log):
```text
NAME                           ACTION   AGE
cart-allow-storefront-bff      ALLOW    11s
product-allow-storefront-bff   ALLOW    11s
```

---

## BƯỚC 4: Demo Phân Quyền Thành Công (Allowed - Bước 8)
*Giới thiệu: "Bây giờ em sẽ thử gửi request từ dịch vụ `storefront-bff` tới cổng quản trị (`8090`) của dịch vụ `product`. Yêu cầu này phải thành công."*

### Lệnh gõ trực tiếp:
```bash
kubectl exec -n yas-dev deploy/storefront-bff -- wget -S --spider http://product.yas-dev:8090/actuator/health
```

### Phương án dự phòng (Backup Log):
```text
Connecting to product.yas-dev:8090 (10.105.5.107:8090)
  HTTP/1.1 200 OK
  x-content-type-options: nosniff
  server: envoy
  connection: close
```
*Nhấn mạnh: Máy chủ trả về `HTTP/1.1 200 OK` và header `server: envoy` chứng tỏ cuộc gọi hợp lệ và đi qua proxy an toàn.*

---

## BƯỚC 5: Demo Phân Quyền Bị Chặn (Denied - Bước 9)
*Giới thiệu: "Để kiểm tra tính bảo mật, em sẽ gọi thử từ dịch vụ khác là `backoffice-bff` tới cùng địa chỉ của `product`. Cuộc gọi này bắt buộc phải bị chặn."*

### Lệnh gõ trực tiếp:
```bash
kubectl exec -n yas-dev deploy/backoffice-bff -- wget -S --spider http://product.yas-dev:8090/actuator/health
```

### Phương án dự phòng (Backup Log):
```text
Connecting to product.yas-dev:8090 (10.105.5.107:8090)
  HTTP/1.1 403 Forbidden
wget: server returned error: HTTP/1.1 403 Forbidden
```
*Nhấn mạnh: Envoy proxy phía nhận đã tự động chặn đứng yêu cầu và trả về lỗi `403 Forbidden` do `backoffice-bff` không có trong danh sách được phép.*

---

## BƯỚC 6: Cấu hình và Demo Retry Policy (Bước 7 & 10)
*Giới thiệu: "Em sẽ cấu hình cơ chế tự động gọi lại (Retry Policy) cho dịch vụ `tax` để tăng tính chống chịu lỗi của hệ thống. Khi gặp lỗi 5xx, client proxy sẽ tự động thử lại 3 lần."*

### Lệnh gõ trực tiếp:
```bash
# 1. Cấu hình VirtualService chứa Retry Policy
kubectl apply -f istio/virtual-service-retry.yaml

# 2. Giả lập lỗi 500 trên Pod tax bằng EnvoyFilter để test
kubectl apply -f envoy-filter-fault.yaml

# 3. Lấy tên Pod của tax để chuẩn bị xem log
kubectl get pods -n yas-dev | grep tax

# 4. Gửi request test từ storefront-bff tới tax
kubectl exec -n yas-dev deploy/storefront-bff -- wget -S --spider http://tax.yas-dev:80/

# 5. Xem log của Pod tax để thấy 4 cuộc gọi mạng liên tiếp
kubectl logs -n yas-dev <tên-pod-tax> -c istio-proxy | tail -n 20
```

### Phương án dự phòng (Backup Log):
Show log in ra của Pod `tax`:
```text
[2026-07-04T12:34:27.864Z] "GET / HTTP/1.1" 500 FI fault_filter_abort - "-" 0 18 0 - "-" "Wget" "f7d8afbf-6017-9c3a-ba90-003c098042a0" "tax.yas-dev:80" "-" inbound|80|| - 10.244.2.37:80 10.244.2.44:51774 outbound_.80_._.tax.yas-dev.svc.cluster.local default
[2026-07-04T12:34:27.896Z] "GET / HTTP/1.1" 500 FI fault_filter_abort - "-" 0 18 0 - "-" "Wget" "f7d8afbf-6017-9c3a-ba90-003c098042a0" "tax.yas-dev:80" "-" inbound|80|| - 10.244.2.37:80 10.244.2.44:51776 outbound_.80_._.tax.yas-dev.svc.cluster.local default
[2026-07-04T12:34:27.931Z] "GET / HTTP/1.1" 500 FI fault_filter_abort - "-" 0 18 0 - "-" "Wget" "f7d8afbf-6017-9c3a-ba90-003c098042a0" "tax.yas-dev:80" "-" inbound|80|| - 10.244.2.37:80 10.244.2.44:51792 outbound_.80_._.tax.yas-dev.svc.cluster.local default
[2026-07-04T12:34:28.031Z] "GET / HTTP/1.1" 500 FI fault_filter_abort - "-" 0 18 0 - "-" "Wget" "f7d8afbf-6017-9c3a-ba90-003c098042a0" "tax.yas-dev:80" "-" inbound|80|| - 10.244.2.37:80 10.244.2.44:51802 outbound_.80_._.tax.yas-dev.svc.cluster.local default
```
*Nhấn mạnh: Có thể thấy 4 yêu cầu có cùng Request ID `f7d8afbf-...` được gửi liên tiếp trong khoảng 0.2 giây. Điều này chứng minh cơ chế Retry đã hoạt động chính xác.*

```bash
# 6. DỌN DẸP hệ thống: Gỡ bỏ cấu hình giả lập lỗi
kubectl delete -f envoy-filter-fault.yaml
```

---

## BƯỚC 7: Sơ Đồ Mạng mTLS Trên Kiali Dashboard (Bước 11)
*Giới thiệu: "Cuối cùng, em xin trình bày sơ đồ kiến trúc mạng trực quan (Topology) của hệ thống thông qua Kiali Dashboard, nơi hiển thị rõ luồng traffic và các ổ khóa mã hóa bảo mật."*

### Lệnh gõ trực tiếp:
```bash
# 1. Thực hiện port-forward dẫn cổng Kiali (chạy ở máy local hoặc WSL)
kubectl port-forward --address 0.0.0.0 -n istio-system svc/kiali 20001:20001
```
2. Mở trình duyệt truy cập: **`http://localhost:20001/kiali`** (hoặc dùng IP máy ảo).
3. Hướng dẫn thao tác trực tiếp trên giao diện:
   - Click chọn **Traffic Graph** ở menu trái.
   - Chọn Namespace là **`yas-dev`**.
   - Tại menu **Display**, tích chọn **`Security`** để hiển thị hình ổ khóa 🔒 mTLS.
   - Chỉ cho giảng viên thấy các biểu tượng ổ khóa màu đen/trắng nằm trực tiếp trên các đường mũi tên màu xanh nối giữa các Pod.
