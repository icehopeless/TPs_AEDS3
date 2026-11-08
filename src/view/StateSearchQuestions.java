package view;

public class StateSearchQuestions extends StateView {

    public StateSearchQuestions(){
        super(true);

        if(!singlePwd.ultimaPagina("Buscar perguntas")){
            singlePwd.push("Buscar perguntas");
        }

        System.out.println(singlePwd.getPwd());
    }

    @Override
    public void draw() {

        System.out.print(Layouts.SearchQuestions.getLayout());
        
        byte option = lerOpcao();

        if(option == 0){
            context.changeState(new StateRecentQuestions(0));
        }else if(option == 1){
            context.changeState(new StateByKeyWordQuestions(false,0));
        }else if(option == 2){
            singlePwd.pop();
            context.changeState(new StateMain());
        }else{
            context.changeState(new StateSearchQuestions());
        }
    }
}
