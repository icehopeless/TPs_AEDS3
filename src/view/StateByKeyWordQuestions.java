package view;

import java.util.ArrayList;

import model.Pergunta;

public class StateByKeyWordQuestions extends StateView{
    private boolean isLoaded;
    private static ArrayList<Pergunta> questions;
    private int indexPage;

    public StateByKeyWordQuestions (boolean isL, int indexPage){
        super(true);
        this.isLoaded = isL;
        this.indexPage = indexPage;
        if(!singlePwd.ultimaPagina("Pergutnas por palavras chaves")){
            singlePwd.push("Pergutnas por palavras chaves");
        }

        System.out.println(singlePwd.getPwd());
    }

    @Override
    public void draw() {
        try {
            
        if(!isLoaded){
            System.out.print(Layouts.SearchByKeyword.getLayout());
            String words = scan.nextLine().strip();
            if(words.equalsIgnoreCase("ret")){
                singlePwd.pop();
                context.changeState(new StateSearchQuestions());
                return;
            }
            questions = control.readPerguntasByWords(words);
        }

            System.out.println(Layouts.DesignTop.getLayout());
            for(int i = indexPage; i < questions.size() && i < (indexPage + 10); i++){
                String primeiraLinha = questions.get(i).getPergunta().split("\n")[0];
                System.out.println("|  (" + (i + 1) + ") " + primeiraLinha);
                System.out.println("|      Criada por " + control.nomeUsuario(questions.get(i).getIdUsuario()) + " em " + questions.get(i).getCriacaoFormatada());
                System.out.println("|");
            }

            if(questions.size() > 0){
                System.out.println("|  Página " + (indexPage + 1) + " de " + questions.size());
            }

            System.out.println("|");
            System.out.println("|  [A] Página anterior    [B] Próxima página    [R] Retornar");
            System.out.println("|  Ou digite o número da pergunta para abri-la");
            System.out.println("|");
            System.out.println("|  Para realizar outra pergunta precione qualquer outra tecla");
            System.out.println(Layouts.DesignBottom.getLayout());
            System.out.print(Layouts.LINECOMAND.getLayout());
            
            String opcao = scan.nextLine().strip().toUpperCase();
            int numero = paraNumero(opcao);

            if(opcao.equals("A") && indexPage > 0){
                context.changeState(new StateByKeyWordQuestions(true,indexPage - 10));
            }else if(opcao.equals("B") && indexPage + 10 < questions.size()){
                context.changeState(new StateByKeyWordQuestions(true,indexPage + 10));
            }else if(opcao.equals("R")){
                singlePwd.pop();
                context.changeState(new StateSearchQuestions());
            }else if(numero >= 1 && numero <= indexPage + 9){
                // ao voltar da pergunta reabre a página da listagem
                context.changeState(new StateQuestion(questions.get(numero - 1).getId(), () -> new StateByKeyWordQuestions(true,indexPage)));
            }else{
                context.changeState(new StateByKeyWordQuestions(false, 0));
            }
            
        
        } catch (Exception e) {
            context.reportFail(e, true);
        }
        
    }

}
