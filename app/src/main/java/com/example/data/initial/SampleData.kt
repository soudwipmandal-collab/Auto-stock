package com.example.data.initial

import com.example.data.model.InventoryItem
import com.example.data.model.StockTransaction
import com.example.data.model.TransactionType

object SampleData {
    fun getInitialInventory(): List<InventoryItem> {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600000
        val oneDayAgo = now - 86400000
        val twoDaysAgo = now - 172800000

        return listOf(
            // SPARE PARTS - Indian Auto Components
            InventoryItem(
                sku = "SKU-SPK-9921",
                barcode = "8901020304050",
                name = "Bosch Super 4 Multi-Ground Spark Plug (W7DTC)",
                category = "Spare Parts",
                subcategory = "Engine",
                fitment = "Maruti Swift, Dzire, Baleno, WagonR (K12B Engine)",
                quantity = 4, // LOW STOCK alert (threshold 10)
                minStockThreshold = 10,
                costPrice = 220.00,
                sellingPrice = 380.00,
                locationRack = "Rack A-01 / Bin 14",
                supplier = "Bosch India Automotive (Bengaluru)",
                description = "Multi-ground electrode spark plug with pure copper core for high fuel efficiency & smooth Indian city idling.",
                unit = "Units",
                lastRestockedTimestamp = twoDaysAgo
            ),
            InventoryItem(
                sku = "SKU-BRK-4012",
                barcode = "8901020304067",
                name = "Uno Minda Front Disc Brake Pads Set",
                category = "Spare Parts",
                subcategory = "Brakes",
                fitment = "Tata Nexon, Punch, Altroz, Tiago (BS6 Models)",
                quantity = 0, // OUT OF STOCK alert
                minStockThreshold = 6,
                costPrice = 650.00,
                sellingPrice = 1150.00,
                locationRack = "Rack B-03 / Shelf 2",
                supplier = "Uno Minda Ltd (Gurugram)",
                description = "OE-grade ceramic-metallic friction material formulated for Indian stop-and-go highway traffic.",
                unit = "Sets",
                lastRestockedTimestamp = oneDayAgo
            ),
            InventoryItem(
                sku = "SKU-OIL-5050",
                barcode = "8901020304074",
                name = "Castrol Activ 4T 20W-40 Synthetic Blend (1L)",
                category = "Spare Parts",
                subcategory = "Fluids",
                fitment = "Hero Splendor, HF Deluxe, Passion, Honda Shine, Bajaj Platina",
                quantity = 28,
                minStockThreshold = 10,
                costPrice = 340.00,
                sellingPrice = 465.00,
                locationRack = "Aisle 4 / Oil Rack D-01",
                supplier = "Castrol India Distributor (Mumbai)",
                description = "Actibond molecules cling to critical motorcycle engine components providing 3X continuous protection.",
                unit = "Litres",
                lastRestockedTimestamp = now
            ),
            InventoryItem(
                sku = "SKU-TYR-2254",
                barcode = "8901020304081",
                name = "MRF ZVTV 185/65 R15 88H Tubeless Car Tyre",
                category = "Spare Parts",
                subcategory = "Tyres",
                fitment = "Maruti Suzuki Swift, Baleno, Dzire, Hyundai i20, Honda Amaze",
                quantity = 2, // LOW STOCK (threshold 6)
                minStockThreshold = 6,
                costPrice = 3600.00,
                sellingPrice = 4850.00,
                locationRack = "Tyre Bay T-08",
                supplier = "MRF Tyres & Rubber (Chennai)",
                description = "Special silica polymer tread compound engineered for long durability on Indian roads and wet braking.",
                unit = "Units",
                lastRestockedTimestamp = oneDayAgo
            ),
            InventoryItem(
                sku = "SKU-BAT-1209",
                barcode = "8901020304111",
                name = "Exide Mileage ML38B20R Car Battery (35Ah, 55M Warranty)",
                category = "Spare Parts",
                subcategory = "Electrical",
                fitment = "Maruti Alto K10, WagonR, Celerio, S-Presso, Hyundai Santro",
                quantity = 1, // LOW STOCK (threshold 4)
                minStockThreshold = 4,
                costPrice = 3400.00,
                sellingPrice = 4650.00,
                locationRack = "Battery Zone B-02",
                supplier = "Exide Industries (Kolkata)",
                description = "Robust grid design with side-vented spill-proof cover engineered for extreme Indian summer heat.",
                unit = "Units",
                lastRestockedTimestamp = twoDaysAgo
            ),
            InventoryItem(
                sku = "SKU-CHN-5200",
                barcode = "8901020304104",
                name = "Hero Genuine Heavy Duty Drive Chain & Sprocket Kit",
                category = "Spare Parts",
                subcategory = "Transmission",
                fitment = "Hero Splendor Plus, Passion Pro, HF Deluxe, Super Splendor",
                quantity = 14,
                minStockThreshold = 6,
                costPrice = 620.00,
                sellingPrice = 890.00,
                locationRack = "Aisle 3 / Bin C-11",
                supplier = "Hero MotoCorp Genuine Spares Hub (Delhi)",
                description = "Heat-treated carbon steel sprocket with factory-lubricated seamless roller chain.",
                unit = "Sets",
                lastRestockedTimestamp = oneDayAgo
            ),
            InventoryItem(
                sku = "SKU-LGT-1002",
                barcode = "8901020304098",
                name = "Lumax H4 12V 60/55W All-Weather Halogen Headlamp Bulb",
                category = "Spare Parts",
                subcategory = "Electrical",
                fitment = "Universal Indian Cars & Bikes (Maruti, Hyundai, Tata, Bajaj, TVS)",
                quantity = 35,
                minStockThreshold = 12,
                costPrice = 110.00,
                sellingPrice = 190.00,
                locationRack = "Aisle 1 / Shelf A-07",
                supplier = "Lumax Auto Technologies (Pune)",
                description = "High-vibration resistant dual filament design for optimum highway visibility during monsoons.",
                unit = "Units",
                lastRestockedTimestamp = oneHourAgo
            ),

            // CARS INVENTORY (Indian Market)
            InventoryItem(
                sku = "CAR-TAT-0101",
                barcode = "8901020304128",
                name = "Tata Nexon Fearless+ S DT Petrol (1.2L Turbo)",
                category = "Cars",
                subcategory = "SUV",
                fitment = "Daytona Grey / White Dual Tone, 2024 BS6 Phase-2",
                quantity = 2,
                minStockThreshold = 1,
                costPrice = 1120000.00,
                sellingPrice = 1349000.00,
                locationRack = "Showroom Bay D-01",
                supplier = "Tata Motors Passenger Vehicles OEM",
                description = "1.2L Revotron Turbocharged 120 PS, 10.25\" Touchscreen with wireless Android Auto, 5-Star BNCAP.",
                unit = "Units",
                lastRestockedTimestamp = now
            ),
            InventoryItem(
                sku = "CAR-MAH-0202",
                barcode = "8901020304135",
                name = "Mahindra Thar LX 4x4 Hard Top Diesel (2.2L mHawk)",
                category = "Cars",
                subcategory = "4x4 SUV",
                fitment = "Napoli Black, 2024 Model",
                quantity = 1, // LOW STOCK
                minStockThreshold = 2,
                costPrice = 1480000.00,
                sellingPrice = 1725000.00,
                locationRack = "Showroom Bay M-02",
                supplier = "Mahindra & Mahindra OEM Direct",
                description = "2.2L mHawk 130 Diesel, Shift-on-the-Fly 4WD, mechanical locking differential, IP54 water-resistant cabin.",
                unit = "Units",
                lastRestockedTimestamp = oneDayAgo
            ),
            InventoryItem(
                sku = "CAR-MAR-0303",
                barcode = "8901020304142",
                name = "Maruti Suzuki Swift ZXi+ Dual Tone 1.2L",
                category = "Cars",
                subcategory = "Hatchback",
                fitment = "Luster Blue with Midnight Black Roof, 2024",
                quantity = 3,
                minStockThreshold = 2,
                costPrice = 740000.00,
                sellingPrice = 895000.00,
                locationRack = "Showroom Bay S-03",
                supplier = "Maruti Suzuki India Ltd Direct",
                description = "All-new Z-Series 1.2L Engine (24.8 km/l mileage), 6 Airbags standard, 9-inch SmartPlay Pro+.",
                unit = "Units",
                lastRestockedTimestamp = now
            ),
            InventoryItem(
                sku = "CAR-HYU-0404",
                barcode = "8901020304180",
                name = "Hyundai Creta SX (O) 1.5L MPi Petrol IVT",
                category = "Cars",
                subcategory = "Mid-SUV",
                fitment = "Abyss Black Pearl, 2024 Facelift",
                quantity = 0, // OUT OF STOCK
                minStockThreshold = 1,
                costPrice = 1610000.00,
                sellingPrice = 1875000.00,
                locationRack = "Showroom Bay H-04",
                supplier = "Hyundai Motor India Ltd (Chennai)",
                description = "Dual 10.25-inch connected screens, Level-2 ADAS suite (19 features), panoramic sunroof, Bose 8-speakers.",
                unit = "Units",
                lastRestockedTimestamp = twoDaysAgo
            ),

            // BIKES & TWO-WHEELERS (Indian Market)
            InventoryItem(
                sku = "BIK-ROY-0101",
                barcode = "8901020304159",
                name = "Royal Enfield Classic 350 Stealth Black (Dual ABS)",
                category = "Bikes",
                subcategory = "Cruiser",
                fitment = "Stealth Black with Matte Alloy Wheels, 2024 J-Series",
                quantity = 2,
                minStockThreshold = 1,
                costPrice = 182000.00,
                sellingPrice = 221000.00,
                locationRack = "Moto Bay RE-01",
                supplier = "Royal Enfield Hub (Chennai)",
                description = "349cc J-Series fuel-injected engine, signature thumping beat, Tripper turn-by-turn navigation pod.",
                unit = "Units",
                lastRestockedTimestamp = now
            ),
            InventoryItem(
                sku = "BIK-HER-0202",
                barcode = "8901020304166",
                name = "Hero Splendor+ XTEC 2.0 i3S 97.2cc (OBD-2)",
                category = "Bikes",
                subcategory = "Commuter",
                fitment = "Black with Silver Accent Stripes, 2024 i3S",
                quantity = 5,
                minStockThreshold = 2,
                costPrice = 65000.00,
                sellingPrice = 79900.00,
                locationRack = "Moto Bay HS-02",
                supplier = "Hero MotoCorp Regional Hub",
                description = "India's highest selling 97.2cc engine (73 km/l mileage), digital meter with Bluetooth call & SMS alerts.",
                unit = "Units",
                lastRestockedTimestamp = oneDayAgo
            ),
            InventoryItem(
                sku = "BIK-HON-0303",
                barcode = "8901020304173",
                name = "Honda Activa 6G H-Smart Deluxe 110cc",
                category = "Bikes",
                subcategory = "Scooter",
                fitment = "Pearl Siren Blue, Smart Key Edition, 2024",
                quantity = 1, // LOW STOCK
                minStockThreshold = 3,
                costPrice = 68500.00,
                sellingPrice = 82600.00,
                locationRack = "Moto Bay HA-03",
                supplier = "Honda Motorcycle & Scooter India (HMSI)",
                description = "Keyless Smart Key with Smart Safe anti-theft, silent ACG starter motor, telescopic front suspension.",
                unit = "Units",
                lastRestockedTimestamp = now
            ),
            InventoryItem(
                sku = "BIK-BAJ-0404",
                barcode = "8901020304197",
                name = "Bajaj Pulsar NS200 Dual Channel ABS (USD Forks)",
                category = "Bikes",
                subcategory = "Street Naked",
                fitment = "Glossy Ebony Black with Red Decals, 2024",
                quantity = 0, // OUT OF STOCK
                minStockThreshold = 1,
                costPrice = 122000.00,
                sellingPrice = 148500.00,
                locationRack = "Moto Bay BP-04",
                supplier = "Bajaj Auto Ltd (Pune)",
                description = "199.5cc Triple Spark DTS-i 4-valve liquid cooled engine (24.5 PS), 33mm upside-down front forks.",
                unit = "Units",
                lastRestockedTimestamp = twoDaysAgo
            )
        )
    }

    fun getInitialTransactions(): List<StockTransaction> {
        val now = System.currentTimeMillis()
        return listOf(
            StockTransaction(
                itemId = 1,
                itemName = "Bosch Super 4 Multi-Ground Spark Plug",
                sku = "SKU-SPK-9921",
                category = "Spare Parts",
                type = TransactionType.STOCK_OUT.name,
                quantityDelta = -6,
                previousQuantity = 10,
                newQuantity = 4,
                reasonOrNote = "Garage Service Bay job #IND-4812 (Maruti Swift periodic service)",
                timestamp = now - 1800000
            ),
            StockTransaction(
                itemId = 2,
                itemName = "Uno Minda Front Disc Brake Pads Set",
                sku = "SKU-BRK-4012",
                category = "Spare Parts",
                type = TransactionType.STOCK_OUT.name,
                quantityDelta = -4,
                previousQuantity = 4,
                newQuantity = 0,
                reasonOrNote = "Counter retail billing for Tata Nexon taxi fleet repair",
                timestamp = now - 7200000
            ),
            StockTransaction(
                itemId = 3,
                itemName = "Castrol Activ 4T 20W-40 Synthetic Blend",
                sku = "SKU-OIL-5050",
                category = "Spare Parts",
                type = TransactionType.STOCK_IN.name,
                quantityDelta = +16,
                previousQuantity = 12,
                newQuantity = 28,
                reasonOrNote = "Received GST Supplier Invoice #CAS-MUM-9943",
                timestamp = now - 14400000
            ),
            StockTransaction(
                itemId = 9,
                itemName = "Mahindra Thar LX 4x4 Hard Top Diesel",
                sku = "CAR-MAH-0202",
                category = "Cars",
                type = TransactionType.STOCK_OUT.name,
                quantityDelta = -1,
                previousQuantity = 2,
                newQuantity = 1,
                reasonOrNote = "Showroom customer vehicle retail delivery #THAR-2024-08",
                timestamp = now - 86400000
            )
        )
    }
}
