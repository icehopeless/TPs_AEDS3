package view;
public enum Layouts {
    MainMenu(   Color.PURPLE.getCode() +
                "  ████████╗███████╗██████╗ ███╗   ███╗██╗███╗   ██╗ █████╗ ██╗     ███╗   ███╗███████╗\r\n" + //
                "  ╚══██╔══╝██╔════╝██╔══██╗████╗ ████║██║████╗  ██║██╔══██╗██║     ████╗ ████║██╔════╝\r\n" + //
                "     ██║   █████╗  ██████╔╝██╔████╔██║██║██╔██╗ ██║███████║██║     ██╔████╔██║█████╗\r\n" + //
                "     ██║   ██╔══╝  ██╔══██╗██║╚██╔╝██║██║██║╚██╗██║██╔══██║██║     ██║╚██╔╝██║██╔══╝\r\n" + //
                "     ██║   ███████╗██║  ██║██║ ╚═╝ ██║██║██║ ╚████║██║  ██║███████╗██║ ╚═╝ ██║███████╗\r\n" + //
                "     ╚═╝   ╚══════╝╚═╝  ╚═╝╚═╝     ╚═╝╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝╚══════╝╚═╝     ╚═╝╚══════╝\r\n" + //
                "\r\n" + //
                Color.GREEN.getCode() + 
                "                         Terminal Question & Answer System\r\n" + //
                 Color.PURPLE.getCode() + 
                 "                                Version 2.0.0\r\n" + //
                "\r\n" + //
                Color.GREEN.getCode() +
                "\t┌──────────────────────────────────────────────────────────────┐\r\n" + //
                "\t│                        MAIN MENU                             │\r\n" + //
                "\t├──────────────────────────────────────────────────────────────┤\r\n" + //
                "\t│                                                              │\r\n" + //
                "\t│   [0]  LOGIN                                                 │\r\n" + //
                "\t│   [1]  CRIAR CONTA                                           │\r\n" + //
                "\t│   [2]  SAIR                                                  │\r\n" + //
                "\t│                                                              │\r\n" + //
                "\t└──────────────────────────────────────────────────────────────┘\r\n" + //
                "\r\n" + Color.GREEN.getCode() +" TerminalMe "+ Color.PURPLE.getCode()+"> "),

    LINECOMAND(Color.GREEN.getCode() +" TerminalMe "+ Color.PURPLE.getCode()+"> "),

    UserMain( Color.GREEN.getCode() +
                "┌──────────────────────────────────────────────────────────────┐\r\n" + //        
                "│                                                              │\r\n" + //
                "│   [0]  Minha Área                                            │\r\n" + //
                "│   [1]  Buscar Perguntas                                      │\r\n" + //
                "│                                                              │\r\n" + //
                "│   [2]  Retornar                                              │\r\n" + //
                "│                                                              │\r\n" + //
                "└──────────────────────────────────────────────────────────────┘\r\n" + //
                "\r\n" + Color.GREEN.getCode() +" TerminalMe "+ Color.PURPLE.getCode()+"> "),
    UserArea( Color.GREEN.getCode() +
                "┌──────────────────────────────────────────────────────────────┐\r\n" + //        
                "│                                                              │\r\n" + //
                "│   [0]  Meus dados                                            │\r\n" + //
                "│   [1]  Minhas perguntas                                      │\r\n" + //
                "│   [2]  Minhas respostas                                      │\r\n" + //
                "│   [3]  Minhas avaliações                                     │\r\n" + //
                "│                                                              │\r\n" + //
                "│   [4]  Retornar ao menu anterior                             │\r\n" + //
                "│                                                              │\r\n" + //
                "└──────────────────────────────────────────────────────────────┘\r\n" + //
                "\r\n" + Color.GREEN.getCode() +" TerminalMe "+ Color.PURPLE.getCode()+"> "),   
    UserData( Color.GREEN.getCode() +
                "┌──────────────────────────────────────────────────────────────┐\r\n" + //        
                "│                                                              │\r\n" + //
                "│   [0]  Alterar nome                                          │\r\n" + //
                "│   [1]  Alterar email                                         │\r\n" + //
                "│   [2]  Alterar senha                                         │\r\n" + //
                "│   [3]  Alterar pergunta e resposta de recuperação da senha   │\r\n" + //
                "│   [4]  Apagar Conta                                          │\r\n" + //
                "│                                                              │\r\n" + //
                "│   [5]  Retornar ao menu anterior                             │\r\n" + //
                "│                                                              │\r\n" + //
                "└──────────────────────────────────────────────────────────────┘\r\n" + //
                "\r\n" + Color.GREEN.getCode() +" TerminalMe "+ Color.PURPLE.getCode()+"> "),                
    UserQuestions( Color.GREEN.getCode() +
                "┌──────────────────────────────────────────────────────────────┐\r\n" + //
                "│                                                              │\r\n" + //
                "│   [0]  Listar                                                │\r\n" + //
                "│   [1]  Incluir                                               │\r\n" + //
                "│   [2]  Alterar                                               │\r\n" + //
                "│   [3]  Arquivar                                              │\r\n" + //
                "│   [4]  Visualizar pergunta e respostas                       │\r\n" + //
                "│                                                              │\r\n" + //
                "│   [5]  Retornar ao menu anterior                             │\r\n" + //
                "│                                                              │\r\n" + //
                "└──────────────────────────────────────────────────────────────┘\r\n" + //
                "\r\n" + Color.GREEN.getCode() +" TerminalMe "+ Color.PURPLE.getCode()+"> "),
                
    SearchQuestions( Color.GREEN.getCode() +
                "┌──────────────────────────────────────────────────────────────┐\r\n" + //
                "│                                                              │\r\n" + //
                "│   [0]  Perguntas mais recentes                               │\r\n" + //
                "│   [1]  Busca por palavras-chave                              │\r\n" + //
                "│                                                              │\r\n" + //
                "│   [2]  Retornar                                              │\r\n" + //
                "│                                                              │\r\n" + //
                "└──────────────────────────────────────────────────────────────┘\r\n" + //
                "\r\n" + Color.GREEN.getCode() +" TerminalMe "+ Color.PURPLE.getCode()+"> "),

    DesignTop(Color.GREEN.getCode() +
                "┌──────────────────────────────────────────────────────────────┐\r\n" + //        
                "│                                                              │\r\n"  //
    ),

    DesignBottom(Color.GREEN.getCode() +
                "│                                                              │\r\n" + //
                "└──────────────────────────────────────────────────────────────┘\r\n" 
    ),

    PWD( 
                Color.GREEN.getCode() + "┌──┐\r\n" + //
                 "│"+ Color.PURPLE.getCode() + "~{}" +Color.GREEN.getCode()+ "│\r\n" + //
                Color.GREEN.getCode() + "└──┘\r\n" 
    ),

    SearchByKeyword(
        Color.GREEN.getCode() +
        "┌──────────────────────────────────────────────────────────────┐\r\n" +
        "│                                                              │\r\n" +
        "│                    BUSCA POR PALAVRAS-CHAVE                  │\r\n" +
        "│                                                              │\r\n" +
        "│  Informe as palavras-chave para localizar perguntas.         │\r\n" +
        "│                                                              │\r\n" +
        "│  Separe os termos por espaço                                 │\r\n" +
        "│                                                              │\r\n" +
        "│  Digite ret para retornar                                    │\r\n" +
        "└──────────────────────────────────────────────────────────────┘\r\n" +
        "\r\n" + Color.GREEN.getCode() +" TerminalMe "+ Color.PURPLE.getCode()+"> "
    );

    
    private String layout;
    Layouts(String lay){
        this.layout = lay;
    }

    public String getLayout() {
        return layout;
    }
}
