# ABestCrates

ABestCrates to plugin Paper do Minecrafta, ktory pozwala tworzyc i obslugiwac konfigurowalne skrzynki z poziomu komend oraz GUI. Projekt jest rozwijany jako nowoczesny system crates z obsluga kluczy fizycznych, kluczy wirtualnych, nagrod itemowych, nagrod komendowych i podgladu nagrod.

## Status

Aktualnie zaimplementowany jest pierwszy dzialajacy rdzen pluginu:

- tworzenie i usuwanie skrzynek,
- stawianie fizycznej skrzynki w lokacji gracza,
- edytor GUI dla podstawowych ustawien skrzynki,
- podglad nagrod,
- klucze fizyczne jako itemy,
- klucze wirtualne zapisywane w pliku,
- losowanie nagrod po realnej szansie,
- oddzielna wyswietlana szansa nagrody,
- nagrody itemowe,
- nagrody komendowe,
- podstawowe efekty wygranej.

Kolejne moduly, takie jak rozbudowane animacje, hologramy, statystyki, webhooki Discord, pity system, daily crates i API eventow, sa przewidziane jako dalszy rozwoj projektu.

## Wymagania

- Java 21
- Paper API 1.21.x
- Maven

## Budowanie

```bash
mvn -q -DskipTests package
```

Gotowy plik `.jar` znajduje sie w katalogu:

```text
target/ABestCrates-1.0-SNAPSHOT.jar
```

## Komendy

Glowna komenda:

```text
/abestcrates
```

Aliasy:

```text
/acrates
/abc
```

Komendy administratora:

```text
/abestcrates reload
/abestcrates create <nazwa>
/abestcrates delete <nazwa>
/abestcrates spawncrate <nazwa>
/abestcrates edit <nazwa>
/abestcrates givekey <gracz> <crate> <ilosc>
/abestcrates addkeys <gracz> <crate> <ilosc>
/abestcrates removekeys <gracz> <crate> <ilosc>
/abestcrates forceopen <gracz> <crate>
```

## Uprawnienia

```text
abestcrates.admin
abestcrates.use
abestcrates.open
abestcrates.create
abestcrates.givekey
abestcrates.reload
```

## Pliki danych

Plugin zapisuje dane runtime w katalogu pluginu na serwerze:

```text
plugins/ABestCrates/crates.yml
plugins/ABestCrates/virtual-keys.yml
plugins/ABestCrates/locations.yml
```

Domyslne wiadomosci znajduja sie w:

```text
src/main/resources/config.yml
```

## Licencja

Projekt jest objety licencja All Rights Reserved. Szczegoly znajduja sie w pliku `LICENSE`.
