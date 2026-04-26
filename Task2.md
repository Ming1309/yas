# Task2 — Phân tích project & cấu hình JaCoCo

**Ngày:** 2026-04-26

---

## 1. Phân tích cấu trúc project YAS

### Build Tool
- Project dùng **Maven** (Multi-Module Project)
- File cấu hình gốc: `pom.xml` tại root
- Mỗi service có `mvnw`, `mvnw.cmd`, `pom.xml` riêng

### Danh sách service (backend Java)
Tất cả nằm trực tiếp tại root monorepo `n:\DevOp\yas\`:

| Service | Đường dẫn |
|---|---|
| cart | `cart/` |
| product | `product/` |
| order | `order/` |
| customer | `customer/` |
| identity | `identity/` |
| payment | `payment/` |
| payment-paypal | `payment-paypal/` |
| inventory | `inventory/` |
| promotion | `promotion/` |
| search | `search/` |
| rating | `rating/` |
| media | `media/` |
| location | `location/` |
| delivery | `delivery/` |
| tax | `tax/` |
| webhook | `webhook/` |
| recommendation | `recommendation/` |
| backoffice-bff | `backoffice-bff/` |
| storefront-bff | `storefront-bff/` |
| common-library | `common-library/` |

Frontend: `backoffice/`, `storefront/`, `automation-ui/`

### Thư mục test
Mỗi service có cấu trúc `src/` gồm 3 loại:

```
<service>/src/
├── main/      ← source code chính
├── test/      ← unit tests (JUnit + Mockito)
└── it/        ← integration tests (*IT.java, Testcontainers + REST Assured)
```

---

## 2. Cấu hình JaCoCo đo test coverage

### Vấn đề phát hiện
JaCoCo đã được khai báo trong `<pluginManagement>` của `pom.xml` nhưng **chưa được kích hoạt** — plugin trong `pluginManagement` chỉ là template, không tự chạy.

### Thay đổi đã thực hiện
**File:** `n:\DevOp\yas\pom.xml`

Thêm JaCoCo plugin vào `<build><plugins>` với 4 execution:

| Execution ID | Phase | Goal | Mục đích |
|---|---|---|---|
| `prepare-agent` | `initialize` | `prepare-agent` | Inject agent đo unit test |
| `report` | `test` | `report` | Sinh báo cáo unit test (HTML + XML) |
| `prepare-agent-integration` | `pre-integration-test` | `prepare-agent-integration` | Inject agent đo integration test |
| `report-integration` | `verify` | `report-integration` | Sinh báo cáo integration test (HTML + XML) |

### File output sau khi chạy
Mỗi service sinh report tại `target/` của chính nó:

```
<service>/target/site/
├── jacoco/        ← Unit test coverage
│   ├── index.html ← Xem bằng browser
│   └── jacoco.xml ← Dùng cho SonarQube/CI
└── jacoco-it/     ← Integration test coverage
    ├── index.html
    └── jacoco.xml
```

### Lệnh chạy
```bash
# Unit test + coverage (chạy từ root)
mvn test

# Unit test + integration test + coverage đầy đủ
mvn verify

# Chạy 1 service cụ thể từ root
mvn test -pl cart

# Upload coverage lên SonarCloud
mvn verify sonar:sonar
```

---



---

## 3. C?u h�nh JaCoCo Coverage Threshold (Build FAIL n?u < 70%)

### Thay d?i d� th?c hi?n
**File:** `n:\DevOp\yas\pom.xml` � Th�m execution `check` v�o JaCoCo plugin.

```xml
<!-- 5) Ki?m tra coverage t?i thi?u � build FAIL n?u < 70% -->
<execution>
    <id>check</id>
    <phase>verify</phase>
    <goals>
        <goal>check</goal>
    </goals>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.70</minimum>
                    </limit>
                    <limit>
                        <counter>BRANCH</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.70</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</execution>
```

### C�ch ho?t d?ng

| Thu?c t�nh | Gi� tr? | � nghia |
|---|---|---|
| `element` | `BUNDLE` | �p d?ng cho to�n b? module |
| `counter` | `LINE` | �?m s? d�ng code du?c th?c thi |
| `counter` | `BRANCH` | �?m s? nh�nh if/switch du?c th?c thi |
| `value` | `COVEREDRATIO` | T�nh theo t? l? (0.0 -> 1.0) |
| `minimum` | `0.70` | Ngu?ng t?i thi?u 70% |

**Lu?ng:**
1. Agent JaCoCo inject v�o JVM tru?c khi test ch?y
2. Test ch?y ? agent ghi nh?n d�ng/nh�nh du?c th?c thi
3. `report` sinh HTML + XML
4. `check` (phase `verify`) so s�nh v?i ngu?ng 70%
5. N?u < 70% ? **BUILD FAILURE**

### L?nh trigger

```bash
# Trigger coverage check (t? root)
mvn verify

# Ch? 1 service
mvn verify -pl cart

# B? qua check t?m th?i (khi dang ph�t tri?n)
mvn verify -Djacoco.skip=true
```

### V� d? output khi FAIL
```
[ERROR] Rule violated for bundle cart:
        lines covered ratio is 0.45, but expected minimum is 0.70
[INFO] BUILD FAILURE
```

---

## 4. GitHub Actions Workflow � Test & Coverage CI

### File t?o m?i
**Path:** `.github/workflows/test-coverage.yaml`

### Trigger
- Push ho?c Pull Request v�o branch `main`
- Khi c� thay d?i trong: `*/src/**`, `pom.xml`, ho?c file workflow
- C� th? k�ch ho?t th? c�ng (`workflow_dispatch`)

### C�c bu?c (Steps) trong job `Test`

| # | Step | Tool | M?c d�ch |
|---|---|---|---|
| 1 | Checkout code | `actions/checkout@v4` | Clone repo, `fetch-depth=0` cho SonarCloud |
| 2 | Setup JDK & Maven cache | `./.github/workflows/actions` | JDK 25 + cache Maven (composite action c� s?n) |
| 3 | Run Tests & Coverage Check | `mvn verify` | Ch?y unit test + IT test + **jacoco:check** (FAIL n?u < 70%) |
| 4 | Publish Unit Test Report | `dorny/test-reporter@v1` | Hi?n th? JUnit XML k?t qu? l�n GitHub Checks tab |
| 5 | Publish Integration Test Report | `dorny/test-reporter@v1` | Tuong t? cho `*IT.java` |
| 6 | Upload JaCoCo Artifact | `actions/upload-artifact@v4` | Luu file HTML + XML 14 ng�y |
| 7 | Coverage Comment on PR | `madrapps/jacoco-report@v1.6.1` | Comment coverage % l�n Pull Request |

### Lu?ng ho?t d?ng
```
Push / PR
  +-> job: Test
        +- mvn verify          ? ch?y test + jacoco:check (FAIL n?u < 70%)
        +- Publish JUnit XML   ? hi?n th? PASS/FAIL t?ng test case tr�n GitHub UI
        +- Upload artifact     ? luu jacoco.xml + index.html d? download
        +- Comment PR          ? post coverage summary l�n PR comment
```

### L� do d�ng `mvn verify` thay v� `mvn test`
- `mvn test` ch? ch?y unit test (Surefire)
- `mvn verify` ch?y th�m integration test (Failsafe) + `jacoco:check`
- `jacoco:check` d� c?u h�nh ? pom.xml ? t? FAIL n?u coverage < 70%

### Noi luu report tr�n GitHub
| Lo?i | Noi xem |
|---|---|
| JUnit test results | Tab **Checks** ? `Unit/Integration Test Results` |
| JaCoCo HTML report | Tab **Artifacts** ? `jacoco-coverage-report` |
| Coverage comment | PR comment (t? d?ng update khi push th�m) |

### Ph�n bi?t v?i c�c workflow `*-ci.yaml` hi?n c�
| | `test-coverage.yaml` | `cart-ci.yaml` (v� d?) |
|---|---|---|
| Ph?m vi | To�n b? project | Ch? service `cart` |
| Trigger path | `*/src/**` | `cart/**` |
| Phase Build | Kh�ng c� | C� (Docker push) |
| M?c d�ch | �o coverage t?ng th? | CI/CD d?y d? t?ng service |

---

## 5. Th�m Upload JUnit XML Artifact v�o GitHub Actions Workflow

### File thay d?i
**Path:** `.github/workflows/test-coverage.yaml`

### Step d� th�m (d?t sau `Publish Integration Test Report`)

```yaml
- name: Upload JUnit Test Results
  uses: actions/upload-artifact@v4
  if: ${{ always() }}
  with:
    name: junit-test-results
    path: |
      **/target/surefire-reports/TEST-*.xml
      **/target/failsafe-reports/TEST-*.xml
    retention-days: 14
```

### Gi?i th�ch

| Thu?c t�nh | Gi� tr? | � nghia |
|---|---|---|
| `if: always()` | - | Upload k? c? khi test FAIL � d? xem test n�o b? l?i |
| `surefire-reports/` | Unit test | Maven Surefire vi?t XML sau khi ch?y unit test |
| `failsafe-reports/` | Integration test | Maven Failsafe vi?t XML sau khi ch?y `*IT.java` |
| `retention-days` | 14 | Gi? artifact 14 ng�y r?i t? x�a |

### C�ch download artifact

```
GitHub ? repo ? Actions ? [ch?n workflow run] ? Artifacts ? junit-test-results ? Download ZIP
```

---

## 6. Upload JaCoCo Coverage Report Artifact

### File thay doi
**Path:** `.github/workflows/test-coverage.yaml` (step da co san tu buoc 4)

### YAML

```yaml
- name: Upload JaCoCo Coverage Report
  uses: actions/upload-artifact@v4
  if: ${{ always() }}
  with:
    name: jacoco-coverage-report
    path: |
      **/target/site/jacoco/jacoco.xml        # Unit test coverage (XML)
      **/target/site/jacoco-it/jacoco.xml     # Integration test coverage (XML)
      **/target/site/jacoco/index.html        # Unit test coverage (HTML)
      **/target/site/jacoco-it/index.html     # Integration test coverage (HTML)
    retention-days: 14
```

### Giai thich

| File | Loai | Dung cho |
|---|---|---|
| `jacoco/jacoco.xml` | XML | SonarCloud, parse bang CI tools |
| `jacoco-it/jacoco.xml` | XML | Integration test coverage cho SonarCloud |
| `jacoco/index.html` | HTML | Mo bang browser, xem truc quan |
| `jacoco-it/index.html` | HTML | Integration test coverage HTML |

- `if: always()` dam bao upload du test PASS hay FAIL
- `retention-days: 14` giu artifact 14 ngay roi tu xoa

### Cach xem HTML report

```
GitHub -> repo -> Actions -> [chon workflow run] -> Artifacts -> jacoco-coverage-report -> Download ZIP
Giai nen -> mo file index.html bang browser
```

---

## 7. Monorepo � Chi chay test khi service thay doi (Path Filtering)

### Cach 1: `paths` filter (dang dung trong project)

Moi service co 1 workflow rieng, chi trigger khi dung thu muc thay doi.

```yaml
# .github/workflows/cart-ci.yaml
on:
  push:
    branches: [ "main" ]
    paths:
      - "cart/**"
      - "pom.xml"
      - ".github/workflows/cart-ci.yaml"
  pull_request:
    branches: [ "main" ]
    paths:
      - "cart/**"
      - "pom.xml"
      - ".github/workflows/cart-ci.yaml"
```

### Cach 2: `dorny/paths-filter` action (1 workflow cho nhieu service)

```yaml
jobs:
  detect-changes:
    runs-on: ubuntu-latest
    outputs:
      cart:    ${{ steps.filter.outputs.cart }}
      product: ${{ steps.filter.outputs.product }}
    steps:
      - uses: actions/checkout@v4
      - uses: dorny/paths-filter@v3
        id: filter
        with:
          filters: |
            cart:
              - "cart/**"
              - "pom.xml"
            product:
              - "product/**"
              - "pom.xml"

  test-cart:
    needs: detect-changes
    if: ${{ needs.detect-changes.outputs.cart == 'true' }}
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: ./.github/workflows/actions
      - run: mvn verify -pl cart -am --batch-mode
```

### So sanh 2 cach

| | Cach 1 (paths filter) | Cach 2 (paths-filter action) |
|---|---|---|
| So file workflow | 1 file / service | 1 file duy nhat |
| Do phuc tap | Thap | Cao hon |
| Linh hoat | Thap | Cao |
| Dang dung trong project | Co | Chua |
| Phu hop khi | <= 10 service | Nhieu service, logic phuc tap |

**Khuyen nghi:** Project YAS co ~19 service, da co san `*-ci.yaml` -> giu Cach 1. Cach 2 chi dung neu muon hop nhat tat ca vao 1 file.
