// Это однострочный комментарий. Он должен удалиться.
public class test {

    /*
       Это многострочный комментарий.
       Он тоже должен исчезнуть.
    */
    public static void main(String[] args) {
        int myNumber = 100; // Переменная для теста
        String myText = "Привет!";

        printInfo(myNumber, myText);
    }

    public static void printInfo(int num, String str) {
        System.out.println(str + " " + num);
    }
}