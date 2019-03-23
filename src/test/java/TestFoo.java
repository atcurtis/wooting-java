import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.xiphis.wooting.WootingRGB;


public class TestFoo {

  @Test
  public void foo() throws InterruptedException, IOException {

    String[] listed = WootingRGB.listAll();
    Assert.assertNotNull(listed);
    Assert.assertEquals(1, listed.length);

    WootingRGB wooting = WootingRGB.open(listed[0]);
    Assert.assertNotNull(wooting);

    wooting.setDirectRGB(0, 0, WootingRGB.RGB.of(0xffffff));

    Thread.sleep(10000);

    wooting.resetRGB();
    wooting.close();
  }

}
