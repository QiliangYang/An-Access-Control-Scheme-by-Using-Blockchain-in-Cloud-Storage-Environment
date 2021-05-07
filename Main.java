package com.company;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;

import java.math.BigInteger;


public class Main {

    public static Pairing pairing;                      //双线性对初始化元素
    public static TypeACurveGenerator pg;               //TypeA 曲线元素生成器
    public static PairingParameters typeAParams;        //TypeA 曲线参数
    public static int rBits = 160;                      //合数阶群的素数个数
    public static int qBits = 512;                      //每个素数的比特长度


    public static Element  g,h,s;                        //G_p群上的生成元
    public static int N = 50;                             //属性个数
    public static int number = N - 1;
    public static Element X[];                          //数据所有者属性集合

    public static Element Y[];                          //数据使用者属性集合
    public static Element S[];
    public static Element user[];

    public static Element Poly[];
    public static Element Poly1[];
    public static Element Poly2[];
    public static Element T[];
    public static Element T1[];
    public static Element F;
    public static Element E;

    public static long time_setup = 0;                  //setup时间消耗统计
    public static long time_kgen = 0;                   //keygen时间消耗统计
    public static long time_Enc = 0;                    //Enc时间消耗统计
    public static long time_Dec = 0;                    //Dec时间消耗统计


    public static void setup(){              //初始化系统参数
           pg = new TypeACurveGenerator(rBits,qBits);
           typeAParams = pg.generate();
           pairing = PairingFactory.getPairing(typeAParams);
           X = new Element[N];
           Y = new Element[N];
           Poly = new Element[2*N];
           Poly1 = new Element[2*N];
           Poly2 = new Element[2*N];
           g = pairing.getG1().newRandomElement().getImmutable();
           h = pairing.getG1().newRandomElement().getImmutable();
           for(int i = 0;i < N;i++){

               X[i] = pairing.getZr().newElement().setToOne().getImmutable();  //定义数据拥有者所有的属性值均为1
               Y[i] = pairing.getZr().newElement().setToOne().getImmutable();  //定义数据用户的所有属性均为1 方便测试；

           }
           for(int i = 0;i < N*2;i++){
               Poly[i] = pairing.getZr().newElement().setToZero();
               Poly1[i] = pairing.getZr().newElement().setToZero();
               Poly2[i] = pairing.getZr().newElement().setToZero();
           }
           System.out.println("length: " + pairing.getG1().newRandomElement().getLengthInBytes());

    }

    public static void compute_data_owner(){      //(1) 第一步 数据拥有者创建一个多项式


            switch_poly(N);           //计算得到Q(x)的各项系数

            T = new Element[N + 1];            //(1) 随机向量 T
            T1 = new Element[N + 1];           //（1）随机向量 t i
            S = new Element[N + 1];            //(1) 向量S
            for(int i = 0;  i <= N; i++){
                T1[i] = pairing.getZr().newRandomElement().getImmutable();
                T[i]  = g.powZn(T1[i]).getImmutable();
                S[i]  = (h.mul(g.powZn(Poly[i].div(T1[i])))).getImmutable();
            }

    }
    public static void compute_data_user(){  //(2) 数据用户计算得到向量User

            s = pairing.getZr().newRandomElement().getImmutable();
            user = new Element[N+1];
            for(int i = 0; i <= N;i++){
                user[i] = T[i].powZn(s.mul(Y[1].pow(BigInteger.valueOf(i))));
            }

    }

    public static void compute_smart_context(){      //(3)验证
            F = pairing.getG1().newElement().getImmutable();
            for(int i = 0; i <= N;i++){
                if(i == 0){
                    F = user[0];
                }else{
                    F = F.mul(user[i]).getImmutable();
                }
            }

            for(int i = 0; i <= N;i++ ){

                if(i == 0){
                    E = pairing.pairing(S[0],user[0]);
                }else{

                    Element Temp = pairing.pairing(S[i],user[i]);
                    E = E.mul(Temp);

                }
            }

            Element Test = pairing.pairing(F,h);
            if(Test.isEqual(E)){
                System.out.println("compute success!");
            }else{
                System.out.println("compute faild!");
            }


    }


    public static void compute_sm(){
        Element C1 = pairing.getZr().newElement();
        C1.set(2);
        System.out.println("C1: " + C1);
        C1.pow(BigInteger.valueOf(3));
        System.out.println("C1x: " + C1);

        Element b = pairing.getG1().newRandomElement().getImmutable();
        Element v = pairing.getG1().newRandomElement().getImmutable();

        Element pa1 = pairing.pairing(b,b.powZn(pairing.getZr().newElement().set(1)));
        Element pa2 = pairing.pairing(b,b.powZn(pairing.getZr().newElement().set(-2)));
        Element pa3 = pairing.pairing(b,b.powZn(pairing.getZr().newElement().set(1)));

        Element Res = pa1.mul(pa2).mul(pa3);
        System.out.println("Res: " + Res);

    }

    public static void switch_poly(int n){  //计算一个n次多项式的各项系数

       
        //设定初始参数（x-1)
        Poly[0] = pairing.getZr().newElement().set(-1).getImmutable();
        Poly[1] = pairing.getZr().newElement().set(1).getImmutable();
        //int i = 2;
        for(int i = 2; i <=n;i++){
            for(int j = 0;j < i;j++){
                Poly1[j+1] = Poly[j].getImmutable();
            }
            for(int index2 = 0;index2 < i;index2++) {
                Poly2[index2] = Poly[index2].mul(pairing.getZr().newElement().set(-1).getImmutable()).getImmutable();
            }
            for(int index3 = 0;index3 <= i;index3++) {
                Poly[index3] = Poly1[index3].add(Poly2[index3]).getImmutable();
            }

            for(int z = 0;z < N*2; z++){
                Poly1[z] = pairing.getZr().newElement().setToZero().getImmutable();
                Poly2[z] = pairing.getZr().newElement().setToZero().getImmutable();
            }
        }
        

    }
    

    public static void compute_poly(){

        BigInteger P1[] = new BigInteger[ N * 2];
        BigInteger P2[] = new BigInteger[ N * 2];
        BigInteger P3[] = new BigInteger[ N * 2];
        for(int i = 0;i < N*2;i++){
            P1[i] = BigInteger.valueOf(0);
            P2[i] = BigInteger.valueOf(0);
            P3[i] = BigInteger.valueOf(0);
        }

        for(int i = 1; i <= N; i++){
            if(i == 1){
                P1[0] = BigInteger.valueOf(-1);
                P1[1] = BigInteger.valueOf(1);
            }else{

                for(int j = 0; j < N; i++){
                    P2[j+1] = P1[j];
                    P3[j]  =  P1[j].multiply(BigInteger.valueOf(-1));
                }
                P2[0] = BigInteger.valueOf(0);
                for(int z = 0; z <= N*2; z++){
                    P1[z] = P2[z].add(P3[z]);
                }
            }
        }
        for(int i = 0; i <= N*2;i++){
            System.out.print(P1[i] + " ");
        }
        System.out.println();



    }


    //测试主流程函数
    public static void main(String[] args) {  


        Testtime.Timer timer = new Testtime.Timer();
        timer.reset();
        setup();
        time_setup += timer.lap();

        timer.reset();
        compute_data_owner();
        time_kgen += timer.lap();

        timer.reset();
        compute_data_user();
        time_Enc += timer.lap();

        timer.reset();
        compute_smart_context();
        time_Dec += timer.lap();

        System.out.println("set up: "  + (time_setup)+ " ms");
        System.out.println("data owner: "  + (time_kgen)+ " ms");
        System.out.println("data user: "  + (time_Enc) + " ms");
        System.out.println("smart context: "+(time_Dec)+" ms)");

        
    }
}

class Testtime {
    public static class Timer {
        private long startTime=System.currentTimeMillis();

        public void reset(){
            startTime=System.currentTimeMillis();
        }

        public int lap(){
            return (int)(System.currentTimeMillis()-startTime);
        }
    }

    public static void main(String[] args) {

    }

}