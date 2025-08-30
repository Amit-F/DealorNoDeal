package deal.cli;
import deal.core.Engine; import deal.core.GameConfig;

public final class Main {
  public static void main(String[] args) {
    int caseCount = 10;
    for (String a : args) if (a.startsWith("--cases=")) caseCount = Integer.parseInt(a.substring(9));
    var s = new Engine(GameConfig.of(caseCount), 42L).start();
    System.out.println("v2 engine: cases=" + s.cases().size() + ", phase=" + s.phase());
    System.out.println("Legacy v1: ./run-legacy.sh");
  }
}

