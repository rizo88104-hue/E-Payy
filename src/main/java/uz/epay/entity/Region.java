package uz.epay.entity;

public enum Region {
    TOSHKENT_SHAHRI("Toshkent shahri", "г. Ташкент", "Tashkent city"),
    TOSHKENT_VILOYATI("Toshkent viloyati", "Ташкентская область", "Tashkent Region"),
    ANDIJON("Andijon viloyati", "Андижанская область", "Andijan Region"),
    BUXORO("Buxoro viloyati", "Бухарская область", "Bukhara Region"),
    FARGONA("Farg'ona viloyati", "Ферганская область", "Fergana Region"),
    JIZZAX("Jizzax viloyati", "Джизакская область", "Jizzakh Region"),
    XORAZM("Xorazm viloyati", "Хорезмская область", "Khorezm Region"),
    NAMANGAN("Namangan viloyati", "Наманганская область", "Namangan Region"),
    NAVOIY("Navoiy viloyati", "Навоийская область", "Navoiy Region"),
    QASHQADARYO("Qashqadaryo viloyati", "Кашкадарьинская область", "Kashkadarya Region"),
    QORAQALPOGISTON("Qoraqalpog'iston Respublikasi", "Республика Каракалпакстан", "Republic of Karakalpakstan"),
    SAMARQAND("Samarqand viloyati", "Самаркандская область", "Samarkand Region"),
    SIRDARYO("Sirdaryo viloyati", "Сырдарьинская область", "Syrdarya Region"),
    SURXONDARYO("Surxondaryo viloyati", "Сурхандарьинская область", "Surkhandarya Region");

    private final String uz;
    private final String ru;
    private final String en;

    Region(String uz, String ru, String en) {
        this.uz = uz;
        this.ru = ru;
        this.en = en;
    }

    public String uz() {
        return uz;
    }

    public String ru() {
        return ru;
    }

    public String en() {
        return en;
    }
}
