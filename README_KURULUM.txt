============================================================
MOB ABSORB MOD — Minecraft Forge 1.20.1 Kurulum ve Derleme
============================================================

1. GEREKSINIMLER
----------------
- Java 17 (JDK 17 — ornegin Adoptium Temurin 17: https://adoptium.net/)
- Gradle 8.x (veya Forge 1.20.1 MDK'nin icindeki gradlew dosyasi)

2. .JAR DOSYASINI DERLEME (1 KOMUT)
-----------------------------------
Bu klasoru zipten cikarin, klasorun icinde terminal / CMD acin ve su komutu calistirin:

    gradle build

(Eger bilgisayarinizda sistem genelinde Gradle kurulu degilse, resmi Minecraft Forge 1.20.1 MDK zipinden `gradlew`, `gradlew.bat` ve `gradle/` klasorunu buraya kopyalayip `./gradlew build` calistirabilirsiniz.)

Derleme tamamlandiginda hazir mod dosyaniz su konumda olusur:
    build/libs/mobabsorb-1.0.0.jar

Bu `.jar` dosyasini Minecraft'in `mods` klasorune atin ve Forge 1.20.1 ile oyunu acin!

3. OYUN ICI KONTROLLER
----------------------
- Mobu Kucaklama: Elin bosken herhangi bir moba `Shift + Sag Tik` yap.
- 1. Mod (Yeme): Mob kucagindayken `Sag Tik` basili tut (2 sn animasyonlu yeme, aclik doldurur ve mobun gucunu verir).
- 2. Mod (Bataklik Gibi Vucuda Absorbe): Mob kucagindayken `Shift + Sag Tik` basili tut veya `V` tusuna basili tut (3 sn boyunca mob bataklik gibi vucudunun icine cekilerek yutulur ve gucunu verir).
- Gucu Kullanma: `R` tusu (Kontroller menusunden degistirilebilir).
- Mobu Birakma: `G` tusu.

4. DESTEKLENEN MOBLAR
---------------------
- Tum vanilla moblar icin ozel pasif ve aktif gucler (Creeper patlamasi, Enderman isinlanma, Warden Sonic Boom, Blaze/Ghast ates topu, Wither kafasi, Ender Dragon nefesi, Evoker disleri, Shulker mermisi, Orumcek duvara tirmanma, Slime ziplama, Gardiyan diken, Inek efekt temizleme, Murekkep baligi korluk bulutu vb.).
- Diger tum moblar (modlu moblar dahil!) icin can, hasar, hiz, ates/su/ucus ozelliklerine gore otomatik guc kazanimi + `R` ile Oz Patlamasi (Essence Burst)!
