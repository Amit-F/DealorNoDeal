![Build](https://github.com/Amit-F/DealorNoDeal/actions/workflows/build.yml/badge.svg)

# Deal or No Deal 🎲💼  
*A renovation of my original simulation project — now cleaner, more professional, and more fun to play!*

---

## 🌟 Overview
This is a simulation of the game show **Deal or No Deal**, with my own twists:  
- **Legacy version** (the original I wrote years ago, still runnable)  
- **Renovated v2** (refactored, tested, Gradle-based, CI-ready)

The project demonstrates:
- **Software craftsmanship**: refactored codebase, modular design (`core`, `cli`, `analytics`, `legacy`)  
- **Engineering best practices**: Gradle multi-module build, Spotless formatting, JUnit 5 tests, GitHub Actions CI  
- **Gameplay creativity**: counteroffers, custom number of cases, and player-driven rounds  

---

## 🕹 Features
- 🎒 **Legacy v1**: the original simulation (choose custom case counts, custom cases per round).  
- 🚀 **Renovated v2**:
  - Modular architecture (`core`, `cli`, `analytics`)  
  - Flexible **round rules** and **banker offers**  
  - Robust **CLI with argument parsing** (`--cases`, `--seed`, `--help`)  
  - Tested end-to-end (round flow, rules, offers)  
  - CI pipelines verifying both v1 + v2 builds  

Coming soon (Stage 5):
- 💼 Counteroffers as a first-class mechanic  
- 🎉 Final reveal logic (what’s in your case?)  
- 📊 Banker “advisor” stats: expected value, risk factor, and strategy hints  

---

## ▶️ How to Run

### Legacy (v1)
The raw original version is still runnable:

```bash
chmod +x ./run-legacy.sh
./run-legacy.sh
```


### Renovated (v2, Gradle)

Run with Gradle — no IDE setup required:

# Example: play with 10 cases
./gradlew :cli:run --args="--cases=10 --seed=42"

# Or: play with 25 cases
./gradlew :cli:run --args="--cases=25 --seed=123"

Flags:

--cases=N → number of briefcases (10, 25 supported; more coming)

--seed=S → RNG seed for reproducible playthroughs

--help → see usage


## 🛠️ Tech Stack

Java 17+

Gradle 9 (multi-module)

JUnit 5 + AssertJ for tests

Spotless for formatting

GitHub Actions for CI



# 👉 Give it a spin, open a case, and see if you’d take the banker’s offer — Deal… or No Deal?
