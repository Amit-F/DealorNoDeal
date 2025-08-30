package il.ac.tau.sc.software1.examples;

public class Briefcase {


    public enum SuitcaseNum {
        SUITCASE1(1, 0), SUITCASE2(2, 0), SUITCASE3(3, 0),
        SUITCASE4(4, 0), SUITCASE5(5, 0), SUITCASE6(6, 0),
        SUITCASE7(7, 0), SUITCASE8(8, 0), SUITCASE9(9, 0),
        SUITCASE10(10, 0), SUITCASE11(11, 0), SUITCASE12(12, 0),
        SUITCASE13(13, 0), SUITCASE14(14, 0), SUITCASE15(15, 0),
        SUITCASE16(16, 0), SUITCASE17(17, 0), SUITCASE18(18, 0),
        SUITCASE19(19, 0), SUITCASE20(20, 0), SUITCASE21(21, 0),
        SUITCASE22(22, 0), SUITCASE23(23, 0), SUITCASE24(24, 0),
        SUITCASE25(25, 0);

        final int finNumOfSuitcase;
        int eSCVal;

        SuitcaseNum (int number){
            this(number, 0);
        }

        SuitcaseNum (int number, int eSCVal){
            this.finNumOfSuitcase = number;
            this.eSCVal = eSCVal;
        }

        public void seteSCVal(int eSCVal) {
            this.eSCVal = eSCVal;
        }

        public int geteSCVal() {
            return eSCVal;
        }
    }

    @Override
    public String toString() {
        return "Briefcase{"+BRIEFCASENUM.finNumOfSuitcase+'}';
    }

    int numberBriefcase;
    int cashVal;
    SuitcaseNum BRIEFCASENUM;

    Briefcase (int numberBriefcase, int cashVal){
        this.numberBriefcase = numberBriefcase;
        this.cashVal = cashVal;
        SuitcaseNum.valueOf("SUITCASE" + numberBriefcase).seteSCVal(cashVal);
        this.BRIEFCASENUM = SuitcaseNum.valueOf("SUITCASE" + numberBriefcase);
    }


}
