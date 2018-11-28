package com.venus.apigw;

import com.venus.esb.utils.Injects;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: lingminjun
 * Date: 2018-11-27
 * Time: 下午12:59
 */
public class InjectTests {
    static public class TestAObject {
        public Long m1 = 100l;
        public Long m2 = 123l;
    }

    static public class TestBObject {
        private long m1;
        private long m2;

        public long getM1() {
            return m1;
        }

        public void setM1(long m1) {
            this.m1 = m1;
        }

        public long getM2() {
            return m2;
        }

        public void setM2(long m2) {
            this.m2 = m2;
        }
    }

    @Test
    public void testGenAPI() {
        TestAObject aObject = new TestAObject();
        TestBObject bObject = new TestBObject();
        Injects.fill(aObject,bObject);
        System.out.println("m1=" + bObject.m1 + "; m2=" + bObject.m2);

        bObject.m1 += 1;
        bObject.m2 += 1;
        TestAObject cObject = new TestAObject();
        Injects.fill(bObject,cObject);
        System.out.println("m1=" + cObject.m1 + "; m2=" + cObject.m2);

        String zz = "中文";
        System.out.println(zz.getBytes().length);
        char c = zz.charAt(0);
        System.out.println(c);

//        List<ESBAPIInfo> apis = ESBAPIHelper.generate(AuthService.class,null,true);
//        System.out.println(JSON.toJSONString(apis));
//        Assert.assertTrue(apis.size() > 0);
    }
}
