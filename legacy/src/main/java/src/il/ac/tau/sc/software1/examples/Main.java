
package il.ac.tau.sc.software1.examples;


import java.util.*;
import java.util.concurrent.TimeUnit;

/*
    Welcome to my version of the hit TV show Deal or No Deal!
    In order to get started, all you have to do is hit run!
    A skippable prompt will appear at the start in order to explain the game if you deem necessary.
    In order to speed up the printing of the text, lower the delay parameter in the last method, and rerun the program.
    My recommendation is to set the delay to around 40, but if you are growing impatient or are short on time
    you are welcome to tune it down all the way to 0.
    Enjoy!
 */


public class Main {

    public static void main(String[] args) { // in charge of starting, continuing, and ending the game
        boolean gameOver = false;
        int numOfBriefs;
        wantsExplanation();
        numOfBriefs = initializeGame();
        ArrayList<Briefcase> listOfBriefcases = assignRelValues(numOfBriefs);
        Briefcase playerPick = playerPick(listOfBriefcases);
        while (listOfBriefcases.size() > 1) {
            int briefcasesToRemove = nextRound(listOfBriefcases, playerPick);
            for (int i = 0; i < briefcasesToRemove; i++){
                slowPrint("(You have to pick " + (briefcasesToRemove - i) + " more briefcases)\n");
                Briefcase toReveal = guessBriefcase(listOfBriefcases, playerPick);
                revealBriefcase(toReveal, listOfBriefcases, playerPick);
            }
            float offers = bankOfferAccepted(listOfBriefcases, playerPick);
            if (offers > 0){
                gameOver(listOfBriefcases, playerPick, offers);
                gameOver = true;
                break;
            }
        }
        if (!gameOver) {
            gameOver(listOfBriefcases, playerPick, 0);
        }
    }
    public static int wantsExplanation(){ // asks player if game explanation is necessary + explains if so
        slowPrint("Do you want a brief explanation of the game?\n");
        slowPrint("[Y] Yes\n[N] No\n");
        Scanner scanner = new Scanner(System.in);
        char needsHelp = Character.toUpperCase(scanner.next().charAt(0)); // todo check to see if makes int uppercase
        switch (needsHelp){
            case 'Y':
                slowPrint("You are a contestant on the game show Deal or No Deal! \nIt is your job to choose" +
                        " one briefcase from a selection of up to 25." +
                        " \nEach briefcase contains a cash value from $1 to $1,000,000." +
                        " \nOver the course of the game, you need to eliminates briefcases, periodically being presented" +
                        " with a \"deal\" from The Bank to take a cash amount to quit the game. " +
                        "\nShould you refuse every deal, you are given the chance to trade the case you chose at" +
                        " the outset for the only one left in play at the time; " +
                        "you then win the \namount in the selected case.\n" +
                        "Therefore, your aim is to remove the briefcases containing small amounts of cash, while " +
                        "leaving the big bucks for the end!\n" +
                        "Doing this will also ensure higher bank offers as you progress!\n");
                break;
            case 'N':
                break;
            default:
                slowPrint("That is not a legal choice\n");
                return wantsExplanation();
        }
        slowPrint("Enjoy the game!\n\n");
        return 0;
    }

    public static int initializeGame(){ // chooses how many briefcases will be used
        int numOfBriefs = 0;
        Scanner scanner = new Scanner(System.in);
        slowPrint("Pick a game mode number: \n [1] Custom (win from up to $10 all the way to $1,000,000)" +
                " \n [2] 10 briefcases (win up to $400)\n [3] 25 briefcases (win up to $1,000,000) \n");
        try {
            int gameNumber = scanner.nextInt();
            switch (gameNumber){
                case 1:
                    return initializeCustomGameIF();
                case 2:
                    return 10;
                case 3:
                    return 25;
                default:
                    slowPrint("Please choose an integer between 1 and 3, corresponding to the game type you want\n");
                    return initializeGame();
            }
        } catch (InputMismatchException ime) {
            slowPrint("Please choose an integer between 1 and 3, corresponding to the game type you want\n");
            return initializeGame();
        }

    }

    public static int initializeCustomGameIF() { // chooses how many briefcases will be used in custom game
        slowPrint("How many briefcases do you want there to be? (between 3 and 25, including both)\n");
        Scanner scanner = new Scanner(System.in);
        try {
            int numOfBriefs = scanner.nextInt();
            if (numOfBriefs < 3 || numOfBriefs > 25) {
                return initializeCustomGameIF();
            }
            else {
                return numOfBriefs;
            }
        }
        catch (InputMismatchException ime){
            return initializeCustomGameIF();
        }
    }


        public static ArrayList<Briefcase> assignRelValues(int numOfBriefs){ // assigns values based on # of suitcases
        ArrayList<Briefcase> listOfBriefcases = new ArrayList<>();
        ArrayList<Integer> cashVals = new ArrayList<>(Arrays.asList(1, 5, 10, 25, 50, 75, 100, 200, 300, 400, 500,
                750, 1000, 5000, 10000, 25000, 50000, 75000, 100000, 200000, 300000, 400000, 500000, 750000, 1000000));
        Random rand = new Random();
        for (int i = numOfBriefs; i > 0; i--){
            int randNum = rand.nextInt(i);
            listOfBriefcases.add(new Briefcase(i, cashVals.get(randNum)));
            cashVals.remove(randNum);
        }
        Collections.reverse(listOfBriefcases);
        return listOfBriefcases;
    }

    public static Briefcase playerPick(ArrayList<Briefcase> listOfBriefcases) { // player picks original suitcase
        slowPrint("(Based on your pick, the maximum amount of money you can win is $");
        System.out.printf("%d)\n\n", highestValueRemaining(listOfBriefcases, listOfBriefcases.get(0)));
        slowPrint("It's time to choose your lucky briefcase!\n");
        Scanner scannerPlayerPick = new Scanner(System.in);
        slowPrint("Pick a number between 1 and " + listOfBriefcases.size() + ":\n");
        int pickedBriefcaseNum;
        try {
            pickedBriefcaseNum = scannerPlayerPick.nextInt();
        } catch (InputMismatchException inputMismatchException) {
            slowPrint("That is not a legal choice\n");
            return playerPick(listOfBriefcases);
        }
        if (pickedBriefcaseNum < 1 || pickedBriefcaseNum > listOfBriefcases.size()) {
            slowPrint("That is not a legal choice\n");
            return playerPick(listOfBriefcases);
        }
        slowPrint("You picked Briefcase number " + pickedBriefcaseNum + "!!! \nGood luck!\n\n");
        Briefcase pickedBriefcase = listOfBriefcases.get(pickedBriefcaseNum - 1);
        listOfBriefcases.remove(pickedBriefcaseNum - 1);
        return pickedBriefcase;
    }


    public static int highestValueRemaining(ArrayList<Briefcase> listOfBriefcases, Briefcase playerPick){ // determines highest value briefcase still in play
        int highestVal = 0;
        for (Briefcase bc : listOfBriefcases){
            if (bc.BRIEFCASENUM.eSCVal > highestVal){
                highestVal = bc.BRIEFCASENUM.eSCVal;
            }
        }
        if (highestVal < playerPick.BRIEFCASENUM.eSCVal){
            highestVal = playerPick.BRIEFCASENUM.eSCVal;
        }
        return highestVal;
    }

    public static int lowestValueRemaining(ArrayList<Briefcase> listOfBriefcases, Briefcase playerPick){ // determines lowest value briefcase still in play
        int lowestVal = 1000000;
        for (Briefcase bc : listOfBriefcases){
            if (bc.BRIEFCASENUM.eSCVal < lowestVal){
                lowestVal = bc.BRIEFCASENUM.eSCVal;
            }
        }
        if (lowestVal > playerPick.BRIEFCASENUM.eSCVal){
            lowestVal = playerPick.BRIEFCASENUM.eSCVal;
        }
        return lowestVal;
    }

    public static float averageMoneyLeft(ArrayList<Briefcase> listOfBriefcases, Briefcase playerPick){ // computes avg money per available briefcase
        float sum = 0;
        for (Briefcase bc : listOfBriefcases){
            sum += bc.BRIEFCASENUM.eSCVal;
        }
        sum += playerPick.BRIEFCASENUM.eSCVal;
        return (sum / (listOfBriefcases.size()+1));
    }

    public static ArrayList<Integer> cashValLeft(ArrayList<Briefcase> listOfBriefcases, Briefcase playerPick){ // returns list of all remaining available cash values
        ArrayList<Integer> cashValsLeft = new ArrayList<>();
        for (Briefcase bc : listOfBriefcases){
            cashValsLeft.add(bc.BRIEFCASENUM.eSCVal);
        }
        cashValsLeft.add(playerPick.BRIEFCASENUM.eSCVal);
        Collections.sort(cashValsLeft);
        return cashValsLeft;
    }

    public static int nextRound(ArrayList<Briefcase> listOfBriefcases, Briefcase playerPick) { // initializes next round
        Scanner scannerRound = new Scanner(System.in);
        slowPrint("There are " + (listOfBriefcases.size() - 1) + " briefcases left to open until the " +
                "final showdown...\n");
        slowPrint("The remaining cash prizes still left are: \n" + cashValLeft(listOfBriefcases, playerPick) +
                "\n");
        slowPrint("How many briefcases do you want to get rid of before the bank offer comes in? (between 1 ");
        System.out.printf("and %d)\n", listOfBriefcases.size()-1);
        int numBriefcasesToRemove;
        try {
            numBriefcasesToRemove = scannerRound.nextInt();
        } catch (InputMismatchException inputMismatchException) {
            slowPrint("That is not a legal choice\n");
            return nextRound(listOfBriefcases, playerPick);
        }
        if (numBriefcasesToRemove < 1 || numBriefcasesToRemove > listOfBriefcases.size() - 1) {
            slowPrint("Don't choose less than 1 briefcase, and don't forget to leave at least 1 remaining\n");
            return nextRound(listOfBriefcases, playerPick);
        }
        slowPrint("\nIt's time to discard " + numBriefcasesToRemove + " briefcases!\n"); // todo 1 briefcases
        slowPrint("Don't forget: ");
        slowPrint("The highest value remaining in a briefcase is $" +
                highestValueRemaining(listOfBriefcases, playerPick) + "!\n");
        slowPrint("Hopefully you won't pick that one!\n");
        return numBriefcasesToRemove;
    }

    public static Briefcase guessBriefcase(ArrayList<Briefcase> listOfBriefcases, Briefcase playerPick){ // removes guessed briefcase
        slowPrint("Now you have to open a briefcase. Hopefully it will be the briefcase with ");
        System.out.printf("$%d ", lowestValueRemaining(listOfBriefcases, playerPick));
        slowPrint("and not the one with ");
        System.out.printf("$%d...\n", highestValueRemaining(listOfBriefcases, playerPick));
        Scanner scannerGuess = new Scanner(System.in);
        slowPrint("\nHere is a list of all the remaining cash prizes:\n");
        System.out.println(cashValLeft(listOfBriefcases, playerPick));
        slowPrint("\nAnd here is a list of all the remaining briefcases; choose one of them!\n");
        System.out.println(Arrays.asList(listOfBriefcases.toArray()));
        int briefToDelete;
        try {
            briefToDelete = scannerGuess.nextInt();
        }
        catch (InputMismatchException inputMismatchException){
            slowPrint("That wasn't a legal pick... try again!\n");
            return guessBriefcase(listOfBriefcases, playerPick);
        }
        for (Briefcase bc : listOfBriefcases){
            if (bc.BRIEFCASENUM.finNumOfSuitcase == briefToDelete){
                return listOfBriefcases.remove(listOfBriefcases.indexOf(bc));
            }
        }
        slowPrint("That wasn't a legal pick... try again!\n");
        return guessBriefcase(listOfBriefcases, playerPick);
//        int indexOf = listOfBriefcases.indexOf((Briefcase.SuitcaseNum.valueOf("SUITCASE"+briefToDelete)));// todo find certain bc in array based on num
        //Briefcase b2D = listOfBriefcases.remove(listOfBriefcases.);
//        System.out.println(indexOf);
    }

    public static void revealBriefcase(Briefcase toReveal,  ArrayList<Briefcase> listOfBriefcases,
                                       Briefcase playerPick){ // reveals chosen briefcase
        slowPrint("\nLet's find out what's behind Briefcase number " + toReveal.BRIEFCASENUM.finNumOfSuitcase
                + "...\n");
        System.out.printf("Briefcase{%d} contained\n", toReveal.BRIEFCASENUM.finNumOfSuitcase);
        slowPrint(".\n.\n.\n", 500);
        System.out.printf("$%d!\n", toReveal.BRIEFCASENUM.eSCVal);
        if (toReveal.BRIEFCASENUM.eSCVal > averageMoneyLeft(listOfBriefcases, playerPick)){
            slowPrint("That's unfortunate, this choice hurt the average amount of cash per briefcase, " +
                    "which in turn will lower future bank offers :(\n");
        }
        else {
            slowPrint("Great pick! This will help the bank make higher offers in the future! \n");
        }
    }

    public static float bankOfferAccepted (ArrayList<Briefcase> listOfBriefcases, Briefcase playerPick){ // produces bank offer
        float averageLeft = averageMoneyLeft(listOfBriefcases, playerPick);
        Scanner dOND = new Scanner(System.in);
        slowPrint("\nThe bank just called, and they are willing to make you an offer to stop playing!\n");
        slowPrint("\nThey say that the average amount of money still at play is ");
        System.out.printf("$%.2f", averageLeft);
        slowPrint("\nTherefore, they are willing to pay you ");
        System.out.printf("$%.2f", (.9*averageLeft));
        slowPrint("\n\nYou can either accept this offer or produce a counteroffer");
        slowPrint("\nTime to decide:\nDeal{D}\nNo Deal{N}\nCounteroffer{C}\n");
        char answer = dOND.next().toUpperCase().charAt(0);
        if (answer == 'D'){
            return (float).9*averageLeft;
        }
        else if (answer == 'N'){
            return 0;
        }
        else if (answer == 'C'){
            return counterOffer(listOfBriefcases, playerPick, averageLeft, dOND);
        }
        else {
            slowPrint("Invalid input, try again\n");
            return bankOfferAccepted(listOfBriefcases, playerPick);
        }
    }

    public static void gameOver(ArrayList<Briefcase> listOfBriefcases, Briefcase playerPick, float acceptedDeal){ // initializes game over sequence
        if (acceptedDeal > 0){
            if (acceptedDeal == (float).9*averageMoneyLeft(listOfBriefcases, playerPick)) {
                slowPrint("\nYou chose to accept the bank's deal of ");
                System.out.printf("$%.2f", acceptedDeal);
            }
            else {
                slowPrint("\nThe bank chose to accept your offer of ");
                System.out.printf("$%.2f", acceptedDeal);
            }
            slowPrint("\nCongratulations! Try again next time for a better result!");
            slowPrint("\nYour briefcase contained ");
            System.out.printf("$%d", playerPick.BRIEFCASENUM.eSCVal);
        }
        else{
            slowPrint("\nYou chose not to accept any of the bank's deals...\n");
            slowPrint("Let's see if that was the right choice!\n");
            Scanner finScanner = new Scanner(System.in);
            slowPrint("You have one final decision to make...\n");
            slowPrint("The briefcase you choose now will be the one you will be going home with!\n");
            slowPrint("The choice is up to you - stick with the original briefcase you chose " +
                    "or choose the last one...\n");
            slowPrint("Each briefcase is holding one of the following amounts of cash: "
                    + cashValLeft(listOfBriefcases, playerPick));
            System.out.printf("\n\n[%d] Original briefcase (%s) \n[%d] Last briefcase remaining (%s) \n",
                    playerPick.BRIEFCASENUM.finNumOfSuitcase, playerPick.toString(),
                    listOfBriefcases.get(0).BRIEFCASENUM.finNumOfSuitcase, listOfBriefcases.get(0).toString());
            try {
                byte finalBCPick = finScanner.nextByte();
                if (finalBCPick == playerPick.BRIEFCASENUM.finNumOfSuitcase){
                    goodDealBadDeal(playerPick, ((float).9*averageMoneyLeft(listOfBriefcases, playerPick)), true);
                    slowPrint("The other briefcase contained ");
                    System.out.printf("$%d", listOfBriefcases.get(0).BRIEFCASENUM.eSCVal);
                }
                else{
                    goodDealBadDeal(listOfBriefcases.get(0), ((float).9*averageMoneyLeft(listOfBriefcases, playerPick)),
                            false);
                    slowPrint("The other briefcase contained ");
                    System.out.printf("$%d", playerPick.BRIEFCASENUM.eSCVal);
                }
            }
            catch (InputMismatchException ime){
                slowPrint("Invalid input, try again\n");
                gameOver(listOfBriefcases, playerPick, acceptedDeal);
            }
        }
    }

    public static void goodDealBadDeal(Briefcase finalPlayerPick, float acceptedDeal, boolean instinct){ // reveals last pick to player and determines if it was a good choice
        if (instinct) {
            slowPrint("\nYou chose to stick to your initial gut feeling!\n");
        }
        else {
            slowPrint("\nYou chose to not stick to your initial gut feeling!\n");
        }
        slowPrint("Brave? \nStupid? \nWho knows!\n");
        slowPrint("Let's find out what's behind Briefcase number "
                + finalPlayerPick.BRIEFCASENUM.finNumOfSuitcase + "!\n");
        System.out.printf("Briefcase{%d} contained\n", finalPlayerPick.BRIEFCASENUM.finNumOfSuitcase);
        slowPrint(".\n.\n.\n", 500);
        System.out.printf("$%d!\n", finalPlayerPick.BRIEFCASENUM.eSCVal);
        if (finalPlayerPick.BRIEFCASENUM.eSCVal > (((1.2)*acceptedDeal))){
            slowPrint("Nice job! Always trust your instincts!\n");
        }
        else {
            slowPrint("You should've picked the other one :( \nBetter luck next time!\n");
        }
    }

    public static float counterOffer(ArrayList<Briefcase> listOfBriefcases, Briefcase playerPick,
                                     float acceptedDeal, Scanner dOND) { // initializes counteroffer sequence
        slowPrint("What do you think is a fair price for you to leave the game?\n");
        Random rand = new Random();
        float counterOffer;
        try {
            counterOffer = dOND.nextFloat();
        } catch (InputMismatchException inputMismatchException) {
            slowPrint("That is not a legal choice\n");
            return counterOffer(listOfBriefcases, playerPick, acceptedDeal, dOND);
        }
        if (counterOffer <= (float) .9 * acceptedDeal) {
            return counterOffer;
        } else if (counterOffer > acceptedDeal * 1.25 || counterOffer >=
                highestValueRemaining(listOfBriefcases, playerPick)) {
            slowPrint("Don't be greedy, NO DEAL\n");
            return 0;
        } else {
            if (rand.nextBoolean()) {
                slowPrint("The bank just called, and they accept your deal!\n");
                return counterOffer;
            }
            slowPrint("The bank just called, and they unfortunately said they cannot accept your " +
                    "counteroffer :(\n");
        }
        return 0;

    }

        public static void slowPrint(String output, int delay) { // print to console in a controlled speed
        for (int i = 0; i<output.length(); i++) {
            char c = output.charAt(i);
            System.out.print(c);
            try {
                TimeUnit.MILLISECONDS.sleep(delay);
            }
            catch (Exception e) {
                continue;
            }
        }
    }

    public static void slowPrint(String output) { // method overload - default version for slow prints
        slowPrint(output, 40);
    }

}


