
$projectRoot = "D:\My Learning\PROJECTS\ecommerce-search"

$inputPath  = Join-Path $projectRoot "database\datasets\products.json"
$outputPath = Join-Path $projectRoot "elasticsearch\products-bulk.ndjson"

$data = Get-Content $inputPath -Raw | ConvertFrom-Json

# ── Gender mapping by category (inferred intelligently) ──────────────────────
# female  → categories that are exclusively or predominantly for women
# male    → categories that are exclusively or predominantly for men
# unisex  → gender-neutral / shared categories
$genderMap = @{
    # Women-specific
    "beauty"           = "female"
    "skin-care"        = "female"
    "womens-dresses"   = "female"
    "womens-bags"      = "female"
    "womens-jewellery" = "female"
    "womens-shoes"     = "female"
    "womens-watches"   = "female"
    "tops"             = "female"

    # Men-specific
    "mens-shirts"      = "male"
    "mens-shoes"       = "male"
    "mens-watches"     = "male"
    "motorcycle"       = "male"

    # Unisex / Gender-neutral
    "fragrances"          = "unisex"
    "furniture"           = "unisex"
    "groceries"           = "unisex"
    "home-decoration"     = "unisex"
    "kitchen-accessories" = "unisex"
    "laptops"             = "unisex"
    "mobile-accessories"  = "unisex"
    "smartphones"         = "unisex"
    "tablets"             = "unisex"
    "sports-accessories"  = "unisex"
    "sunglasses"          = "unisex"
    "vehicle"             = "unisex"
}

$lines = foreach ($product in $data.products) {

    # Resolve gender; fall back to "unisex" for any unknown category
    $gender = $genderMap[$product.category]
    if (-not $gender) { $gender = "unisex" }

    # Elasticsearch indexing instruction
    @{
        index = @{
            _index = "products"
            _id    = "$($product.id)"
        }
    } | ConvertTo-Json -Compress -Depth 10

    # Document — fields must match the index mapping
    @{
        id          = $product.id
        title       = $product.title
        description = $product.description
        category    = $product.category
        brand       = $product.brand
        price       = $product.price
        stock       = $product.stock
        tags        = @($product.tags)
        thumbnail   = $product.thumbnail
        rating      = $product.rating
        gender      = $gender
    } | ConvertTo-Json -Compress -Depth 10
}

# UTF-8 without BOM + required trailing newline (Elasticsearch Bulk API requirement)
$content = ($lines -join "`n") + "`n"

[System.IO.File]::WriteAllText(
    $outputPath,
    $content,
    [System.Text.UTF8Encoding]::new($false)
)

Write-Host "Products processed : $($data.products.Count)"
Write-Host "NDJSON lines       : $($lines.Count)"
Write-Host "File created       : $outputPath"
