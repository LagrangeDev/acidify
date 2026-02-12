import org.ntqqrev.acidify.runner.main
import java.io.PrintStream

fun main() {
    val utf8out = PrintStream(System.out, true, "UTF-8")
    System.setOut(utf8out)
    System.setErr(utf8out)
    main()
}