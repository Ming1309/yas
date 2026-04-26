# CI/CD Pipeline Restructuring Task

**Ngày tạo:** April 26, 2026  
**Mục tiêu:** Cập nhật tất cả workflow files có 2 phases: Test và Build

---

## ✅ Trạng thái: HOÀN THÀNH

---

## 📋 Các thay đổi đã thực hiện

### **1. Các Java/Maven Services (15 files)**
Các services đã được cập nhật cấu trúc 2-phase:

- `cart-ci.yaml`
- `customer-ci.yaml`
- `inventory-ci.yaml`
- `location-ci.yaml`
- `media-ci.yaml`
- `order-ci.yaml`
- `payment-ci.yaml`
- `payment-paypal-ci.yaml`
- `product-ci.yaml`
- `promotion-ci.yaml`
- `rating-ci.yaml`
- `recommendation-ci.yaml`
- `search-ci.yaml`
- `tax-ci.yaml`
- `webhook-ci.yaml`

### **2. BFF Services - Java/Maven (2 files)**
- `backoffice-bff-ci.yaml`
- `storefront-bff-ci.yaml`

### **3. Data Services (1 file)**
- `sampledata-ci.yaml`

### **4. Node.js Services (2 files)**
- `backoffice-ci.yaml`
- `storefront-ci.yaml`

---

## 🔧 Cấu trúc mỗi Workflow

### **Test Phase - các steps:**
```yaml
Test:
  runs-on: ubuntu-latest
  permissions:
    contents: read
    checks: write
  steps:
    - Checkout code
    - Setup JDK/Node.js
    - Run tests (mvn clean install / npm install)
    - Code quality checks (Checkstyle/Prettier)
    - Security scanning (OWASP Dependency Check / npm audit / Trivy)
    - SonarCloud analysis
    - Code coverage reports
```

### **Build Phase - các steps:**
```yaml
Build:
  needs: Test
  runs-on: ubuntu-latest
  if: ${{ github.ref == 'refs/heads/main' }}
  permissions:
    contents: read
    packages: write
  steps:
    - Checkout code
    - Set lowercase image owner
    - Docker login & build image
    - Push to ghcr.io
```

---

## 🎯 Các tính năng chính

✅ **Test phase dependency**: Build job chỉ chạy khi Test job thành công  
✅ **Main branch condition**: Docker image chỉ push lên khi push vào main branch  
✅ **Consistent structure**: Tất cả 20 workflows có cấu trúc giống nhau  
✅ **Quality gates**: Test phase bao gồm tất cả checks trước khi build  
✅ **Container registry**: Push tới ghcr.io với tag `latest`  

---

## 📊 Tóm tắt thay đổi

| Loại Service | Số lượng | Trạng thái |
|---|---|---|
| Java/Maven Microservices | 15 | ✅ Completed |
| Java/Maven BFF | 2 | ✅ Completed |
| Data Services | 1 | ✅ Completed |
| Node.js Services | 2 | ✅ Completed |
| **TỔNG CỘNG** | **20** | **✅ COMPLETED** |

---

## 🚀 Verification

**Cách verify:**
```bash
# 1. Check git diff
git diff

# 2. Xác nhận cấu trúc 2-phase
grep -E "^jobs:|^  Test:|^  Build:|needs: Test|if:.*refs/heads" <workflow-file>

# 3. Xem toàn bộ thay đổi
git status
```

**Kết quả xác nhận:**
- ✅ Tất cả 20 files có Test job tại dòng ~24
- ✅ Tất cả 20 files có Build job tại dòng ~92
- ✅ Build job có `needs: Test` dependency
- ✅ Build job có `if: ${{ github.ref == 'refs/heads/main' }}` condition

---

## 💡 Lưu ý

- **Không cập nhật**: `charts-ci.yaml`, `codeql.yml`, `gitleaks-check.yaml` (không phải microservice CI/CD)
- **Image Owner**: Biến `IMAGE_OWNER` được set trong Build job trước khi sử dụng
- **Container Registry**: Sử dụng ghcr.io (GitHub Container Registry)
- **Runner**: Ubuntu-latest cho tất cả jobs

---

## 📝 Tiếp theo

- [ ] Review workflows trên GitHub UI
- [ ] Push changes lên repository
- [ ] Test bằng cách trigger push trên feature branch
- [ ] Verify Test phase chạy thành công
- [ ] Verify Build phase chỉ chạy trên main branch
- [ ] Monitor GitHub Actions runs
