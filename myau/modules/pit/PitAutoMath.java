package myau.modules.pit;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;
import java.util.function.Consumer;

public final class PitAutoMath {
    private String pending,lastQuestion;
    private long due,lastReceived;
    public synchronized void receive(String message,long now){
        if(message==null)return;
        int index=message.indexOf("QUICK MATHS! Solve: ");
        if(index<0)return;
        String question=message.substring(index+20).trim();
        if(question.equals(lastQuestion)&&now>=lastReceived&&now-lastReceived<10000)return;
        Optional<String> answer=solve(question);
        if(!answer.isPresent())return;
        pending=answer.get();lastQuestion=question;lastReceived=now;
        due=now+250;
    }
    public synchronized void tick(boolean active,long now,Consumer<String> send){
        if(!active){reset();return;}
        if(pending==null||now<due)return;
        String answer=pending;pending=null;
        if(now-due>10000)return;
        send.accept("/ac "+answer);
    }
    public synchronized void reset(){pending=null;lastQuestion=null;due=0;lastReceived=0;}
    public static Optional<String> solve(String expression){
        if(expression==null||expression.length()>128)return Optional.empty();
        try{
            Parser parser=new Parser(expression.replace('\u00d7','*').replace('x','*').replace('\u00f7','/'));
            BigDecimal result=parser.sum();parser.space();
            if(parser.at!=parser.text.length()||result.precision()>64||Math.abs(result.scale())>64)return Optional.empty();
            return Optional.of(result.stripTrailingZeros().toPlainString());
        }catch(IllegalArgumentException|ArithmeticException invalid){return Optional.empty();}
    }
    private static final class Parser {
        final String text;int at,depth;
        Parser(String text){this.text=text;}
        void space(){while(at<text.length()&&Character.isWhitespace(text.charAt(at)))at++;}
        boolean eat(char c){space();if(at<text.length()&&text.charAt(at)==c){at++;return true;}return false;}
        BigDecimal sum(){BigDecimal n=product();for(;;){if(eat('+'))n=n.add(product());else if(eat('-'))n=n.subtract(product());else return n;}}
        BigDecimal product(){BigDecimal n=atom();for(;;){if(eat('*'))n=n.multiply(atom());else if(eat('/'))n=n.divide(atom(),MathContext.DECIMAL64);else return n;}}
        BigDecimal atom(){
            if(++depth>32)throw new IllegalArgumentException("nesting");
            try{
                if(eat('+'))return atom();if(eat('-'))return atom().negate();
                if(eat('(')){BigDecimal n=sum();if(!eat(')'))throw new IllegalArgumentException("parenthesis");return n;}
                space();int start=at;
                while(at<text.length()&&(text.charAt(at)>='0'&&text.charAt(at)<='9'||text.charAt(at)=='.'))at++;
                if(start==at)throw new IllegalArgumentException("number");
                return new BigDecimal(text.substring(start,at));
            }finally{depth--;}
        }
    }
}
