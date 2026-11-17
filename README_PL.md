# AutoCloseCommand — Minecraft 26.3 (Fabric)

## Co robi mod

- Jest modem wyłącznie klienckim.
- Domyślnie jest włączony od razu po uruchomieniu Minecrafta.
- Wykrywa konkretne okno o układzie 54 slotów u góry + 36 slotów ekwipunku gracza (łącznie 90 slotów).
- Rozpoznawanie opiera się wyłącznie na liczbie slotów — mod nie sprawdza kolorów, typów, nazw ani pozycji przedmiotów, więc mogą się one zmieniać i przestawiać.
- Zamyka takie okno przez jego standardową procedurę zamykania (jak Escape), odczekuje 2 ticki gry i dopiero potem wysyła na serwer komendę `autoclicker`. Opóźnienie zapobiega wyścigowi między zamknięciem kontenera a wysłaniem komendy.
- Wysyła komendę tylko raz na jedno otwarcie okna, a nie 20 razy na sekundę.
- Zwykły ekwipunek gracza (klawisz E) nie jest traktowany jako okno docelowe.
- **F8** włącza lub wyłącza automat. Stan przełącznika jest resetowany do „włączony” po ponownym uruchomieniu gry.

Komenda jest wysyłana bezpośrednio jako komenda czatu Minecrafta. Nie pojawia się na ekranie otwarte pole wpisywania czatu, ale serwer otrzymuje tę samą komendę.

## Wymagania

- Minecraft Java **26.3**
- Fabric Loader **0.20.1** lub nowszy
- Fabric API **0.166.2+26.3**
- Java **25**

## Budowanie pliku JAR w Windows

1. Zainstaluj JDK 25.
2. Zainstaluj Gradle 9.7.1 lub nowszy zgodny z Fabric Loom dla Minecrafta 26.3 (np. 9.9.1).
3. Otwórz terminal w folderze tego projektu.
4. Najprościej uruchom `build.bat` dwukrotnym kliknięciem. Skrypt korzysta z JDK wskazanego przez `JAVA_HOME` i wyłącza konfigurację cache Gradle dla zgodności z konfiguracją projektu. Możesz też uruchomić w terminalu:

   ```powershell
   gradle build --no-configuration-cache
   ```

5. Gotowy mod będzie w:

   ```text
   build\libs\autoclosecommand-1.0.0.jar
   ```

6. Włóż plik JAR do folderu `mods` profilu Fabric w launcherze. Upewnij się, że w tym profilu jest też Fabric API.

## Ważne

Mod zareaguje tylko na niestandardowe okno będące ekranem kontenera Minecrafta z dokładnie 90 slotami łącznie (54 u góry + 36 ekwipunku gracza). Jeśli serwer używa innej liczby slotów albo całkowicie niestandardowego ekranu, trzeba będzie dostosować wykrywanie.
