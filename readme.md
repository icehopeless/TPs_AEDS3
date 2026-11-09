# Relatório
# TP3 de AEDs III: TERMINALME, índice invertido

Trabalho prático 3 de Algoritmos e Estruturas de Dados III (PUC Minas, 2026.2). O TP3 continua o
sistema dos TPs anteriores e completa a busca de perguntas: a opção "Busca por palavras-chave", que
estava em construção, agora procura as perguntas pelos termos das palavras-chave. Um índice
invertido, montado com a classe `ListaInvertida` do professor, guarda para cada termo os pares
(idPergunta; TF), e o resultado da busca sai ordenado pelo valor TF×IDF.

As seções sobre o TP3 estão marcadas como tal: [Busca por palavras-chave](#busca-por-palavras-chave-tp3)
nas capturas, as classes novas em [Classes e métodos](#classes-e-métodos), as operações em
[Do TP3](#do-tp3) e o [checklist](#checklist) no final.

## Participantes

| Nome | GitHub |
|---|---|
| Gabriel Agostinho | [icehopeless](https://github.com/icehopeless) |
| Isaque de Sousa | [Ws49](https://github.com/Ws49) |
| Kaio Ferreira Dias | [KaioT2](https://github.com/KaioT2) |
| Natan França | [Nt-0411](https://github.com/Nt-0411) |

## Descrição do sistema

O **TERMINALME** (*Terminal Question & Answer System*) é o nome que o grupo deu ao sistema pedido no
enunciado como AJUDA AÍ 1.0. É uma versão simplificada do StackOverflow, feita em Java para o
terminal. Depois do TP3, o sistema cobre quatro entidades:

- **Usuários**, que se cadastram, entram com email e senha, recuperam a senha por uma pergunta
  secreta e alteram ou apagam os próprios dados.
- **Perguntas**, que pertencem a um usuário. O autor inclui, lista, altera e arquiva as próprias
  perguntas. Qualquer usuário encontra as perguntas de todos de duas formas: na listagem das mais
  recentes, de cinco em cinco, ou pela busca por palavras-chave, de dez em dez, em ordem de TF×IDF.
- **Respostas**, que ligam um usuário a uma pergunta. Qualquer usuário responde a uma pergunta
  ativa. A resposta não pode ser alterada, só arquivada, pelo autor dela ou pelo autor da pergunta.
- **Avaliações**, que registram se um usuário concordou ou discordou de uma resposta. Cada usuário
  avalia uma resposta uma vez só e pode cancelar a avaliação depois.

### Apresentação do sistema

- Vídeo do TP3: https://www.youtube.com/watch?v=TjrVOuUDiCA
- Vídeo do TP2: https://youtu.be/b5HtPXqXGl8
- Vídeo do TP1: https://youtu.be/DJMiSdkOcdA

### Organização do código

O código segue o padrão **MVC**, com um pacote para cada camada:

| Pacote | Papel |
|---|---|
| `model` | as entidades (`User`, `Pergunta`, `Resposta`, `Avaliacao`) e o acesso aos arquivos (`model.AcessDataBase`) |
| `control` | o `Controller`, que valida as regras de negócio e liga a visão ao modelo, e o `SingletonUser`, que guarda o usuário logado |
| `view` | as telas do terminal |
| `Security` | o hash SHA-256 da senha |
| `Formatter` | o `FormatterString`, que quebra textos longos em linhas com o prefixo `\|  ` para caber nas caixas das telas |

As telas usam o padrão **State**:
- cada tela é uma subclasse de `StateView` com um método `draw()`;
- o `ContextView` desenha a tela atual e passa para a próxima quando a tela chama `changeState(...)`.
  Toda tela precisa terminar chamando `changeState`, senão o laço do `render()` acaba e o programa
  fecha;
- o `SingletonPWD` guarda o caminho de navegação mostrado no topo (ex.: `~Início > Buscar perguntas > Perguntas mais recentes`);
- as telas que podem ser abertas de mais de um lugar (a pergunta e a resposta) recebem um
  `Supplier<StateView>` chamado `voltar`, que diz para qual tela o "Retornar" leva.

### Persistência

As quatro entidades são gravadas pela classe genérica `Arquivo<T>` do professor. O programa cria os
arquivos na pasta `dados/`, a partir da pasta onde é executado.

- **Cabeçalho do arquivo:** 12 bytes, com o último ID usado (`int`) e o endereço do primeiro espaço
  livre (`long`).
- **Cada registro:** lápide (`boolean`), tamanho (`short`) e os bytes da entidade.
- **Índice direto:** uma Tabela Hash Extensível de pares (ID; endereço), usada por `read`, `update`
  e `delete`.
- **Espaços livres:** registros excluídos entram numa lista ordenada por tamanho e são
  reaproveitados quando o desperdício fica abaixo de 50 bytes.

| Arquivo | Conteúdo |
|---|---|
| `dados/users/arquivo.db` | registros dos usuários |
| `dados/users/indiceID.diretorio.db`, `indiceID.cestos.db` | índice direto dos usuários (hash extensível) |
| `dados/users/indiceEmail.d.db`, `indiceEmail.c.db` | índice indireto por email (hash extensível) |
| `dados/perguntas/arquivo.db` | registros das perguntas |
| `dados/perguntas/indiceID.diretorio.db`, `indiceID.cestos.db` | índice direto das perguntas |
| `dados/perguntas/relUsuarioPergunta.db` | Árvore B+ do relacionamento 1:N, com pares (idUsuario; idPergunta) |
| `dados/perguntas/indiceData.db` | **Árvore B+ por data**, com pares (criação; idPergunta) das perguntas ativas, em ordem decrescente de data |
| `dados/perguntas/DictionaryQuestions.db` | dicionário do **índice invertido** (TP3): os 4 primeiros bytes guardam quantas perguntas estão indexadas, que é o N do IDF; depois vem cada termo com o endereço do primeiro bloco da lista dele |
| `dados/perguntas/InvertedQuestions.db` | blocos das listas invertidas (TP3): cada bloco guarda até 30 pares (idPergunta; TF) e o endereço do bloco seguinte do mesmo termo |
| `dados/respostas/arquivo.db` | registros das respostas |
| `dados/respostas/indiceID.diretorio.db`, `indiceID.cestos.db` | índice direto das respostas |
| `dados/respostas/relPerguntaResposta.db` | Árvore B+ com pares (idPergunta; idResposta): as respostas de cada pergunta |
| `dados/respostas/relUsuarioResposta.db` | Árvore B+ com pares (idUsuario; idResposta): as respostas de cada usuário |
| `dados/avaliacoes/arquivo.db` | registros das avaliações |
| `dados/avaliacoes/indiceID.diretorio.db`, `indiceID.cestos.db` | índice direto das avaliações |
| `dados/avaliacoes/relUsuarioAvaliacao.db` | Árvore B+ com pares (idUsuario; idAvaliacao): as avaliações de cada usuário |
| `dados/avaliacoes/relPerguntAvaliacao.db` | Árvore B+ com pares (idResposta; idAvaliacao): as avaliações de cada resposta. O nome do arquivo ficou do primeiro rascunho; a chave é a resposta |

### Entidades

**Usuário (`User`)**

| Atributo | Tipo | Observação |
|---|---|---|
| `idUsuario` | `int` | gerado pelo `Arquivo`, sequencial, nunca aparece na tela |
| `nome` | `String` | |
| `email` | `String` | é o login e é único; tem índice indireto na hash extensível |
| `hashSenha` | `String` | SHA-256 da senha |
| `perguntaSecreta` | `String` | usada na recuperação da senha |
| `hashRespostaSecreta` | `int` | hash da resposta secreta |

**Pergunta (`Pergunta`)**

| Atributo | Tipo | Observação |
|---|---|---|
| `id` | `int` | gerado pelo `Arquivo`, sequencial e único no sistema todo |
| `idUsuario` | `int` | chave estrangeira para o autor; não existe `setIdUsuario`, então nunca muda |
| `criacao` | `long` | data e hora da criação, em milissegundos; é a chave do índice por data |
| `alteracao` | `long` | data e hora da última alteração, atualizada a cada alteração e no arquivamento |
| `nota` | `short` | reservada; no TP2 quem recebe avaliações são as respostas, então continua 0 |
| `pergunta` | `String` | o texto, com até 4.000 caracteres e várias linhas |
| `palavrasChave` | `String` | termos separados por `;`, com até 1.000 caracteres |
| `ativa` | `boolean` | `false` quando a pergunta está arquivada |

**Resposta (`Resposta`)**

| Atributo | Tipo | Observação |
|---|---|---|
| `idResposta` | `int` | gerado pelo `Arquivo`, sequencial e independente da pergunta e do usuário |
| `idPergunta` | `int` | chave estrangeira para a pergunta respondida |
| `idUsuario` | `int` | chave estrangeira para o autor da resposta |
| `criacao` | `long` | data e hora da criação, em milissegundos; não muda nem no arquivamento |
| `resposta` | `String` | o texto, com até 4.000 caracteres e várias linhas |
| `nota` | `short` | concordâncias menos discordâncias |
| `avaliacoes` | `short` | quantas avaliações a resposta recebeu |
| `ativa` | `boolean` | `false` quando a resposta está arquivada; arquivada não aparece nas listagens da pergunta |

**Avaliação (`Avaliacao`)**

| Atributo | Tipo | Observação |
|---|---|---|
| `idAvaliacao` | `int` | gerado pelo `Arquivo`, sequencial |
| `idUsuario` | `int` | chave estrangeira para quem avaliou |
| `idResposta` | `int` | chave estrangeira para a resposta avaliada |
| `itSelfAvaliacao` | `boolean` | é o atributo `avaliação` do enunciado: `true` quando o usuário concordou, `false` quando discordou |

### Como compilar e executar

Na raiz do repositório:

```
javac -encoding UTF-8 -d bin $(find src -name "*.java")
java -cp bin Main
```

No Windows, pelo PowerShell:

```
javac -encoding UTF-8 -d bin (Get-ChildItem -Recurse src -Filter *.java).FullName
java "-Dstdout.encoding=UTF-8" -cp bin Main
```

No Windows, pelo Prompt de Comando:

```
chcp 65001
javac -encoding UTF-8 -d bin src\*.java src\Formatter\*.java src\Security\*.java src\control\*.java src\model\*.java src\model\AcessDataBase\*.java src\view\*.java
java -Dstdout.encoding=UTF-8 -cp bin Main
```

O terminal precisa estar em UTF-8 para os acentos e as bordas das caixas aparecerem certos. A
leitura do teclado não depende dessa configuração: o `StateView` cria o `Scanner` com a
codificação que o próprio console informa, como explica a seção "Operações especiais".

## Capturas das telas

O texto digitado pelo usuário aparece depois de cada `TerminalMe >`. As telas do TP2 foram copiadas
de uma execução real do programa, com três usuários de teste: Ana Souza, Bruno Lima e Carla Mendes.

### Menu do usuário logado

A opção "Buscar Perguntas", que no TP1 estava em construção, agora abre a busca.

```
┌───────┐
│~Início│
└───────┘
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│   [0]  Minha Área                                            │
│   [1]  Buscar Perguntas                                      │
│                                                              │
│   [2]  Retornar                                              │
│                                                              │
└──────────────────────────────────────────────────────────────┘

 TerminalMe > 1
```

### Buscar perguntas

```
┌──────────────────────────┐
│~Início > Buscar perguntas│
└──────────────────────────┘
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│   [0]  Perguntas mais recentes                               │
│   [1]  Busca por palavras-chave                              │
│                                                              │
│   [2]  Retornar                                              │
│                                                              │
└──────────────────────────────────────────────────────────────┘

 TerminalMe > 0
```

A opção 1 abre a busca por palavras-chave, descrita logo abaixo.

### Busca por palavras-chave (TP3)

As capturas do TP3 vêm de uma execução real com dois usuários, Ana Souza e Bruno Lima, e quatro
perguntas. As palavras-chave delas repetem os títulos dos livros do exemplo do enunciado, para que
as listas e os valores possam ser conferidos contra ele:

| ID | Pergunta | Autor | Palavras-chave |
|---|---|---|---|
| 1 | Por onde começar a estudar inteligência artificial? | Ana Souza | `introdução;inteligência;artificial` |
| 2 | Como desenvolver inteligência emocional sendo gestor? | Ana Souza | `inteligência;emocional;gestores` |
| 3 | Inteligência no trabalho: como aplicar por meio da inteligência artificial? | Bruno Lima | `inteligência;trabalho;meio;inteligência;artificial` |
| 4 | Que livro de introdução à gestão de equipes vocês recomendam? | Bruno Lima | `introdução;gestão;equipes` |

Depois das quatro inclusões, os arquivos do índice continham as listas abaixo, impressas pelo
método `print()` da `ListaInvertida`. Cada par é (idPergunta; TF). São as mesmas listas do
enunciado; o termo "inteligencia" da pergunta 3 aparece duas vezes em cinco termos, por isso o TF é
0,4.

```
Entidades: 4

LISTAS INVERTIDAS:
introducao: (1;0.33333334) (4;0.33333334)
inteligencia: (1;0.33333334) (2;0.33333334) (3;0.4)
artificial: (1;0.33333334) (3;0.2)
emocional: (2;0.33333334)
gestores: (2;0.33333334)
trabalho: (3;0.2)
meio: (3;0.2)
gestao: (4;0.33333334)
equipes: (4;0.33333334)
```

**Busca com mais de uma palavra.** A consulta foi digitada com acento e letra maiúscula, que a busca
ignora:

```
┌─────────────────────────────────────────────────────────┐
│~Início > Buscar perguntas > Perguntas por palavras-chave│
└─────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│                    BUSCA POR PALAVRAS-CHAVE                  │
│                                                              │
│  Informe as palavras-chave para localizar perguntas.         │
│                                                              │
│  Separe os termos por espaço                                 │
│                                                              │
│  Digite ret para retornar                                    │
└──────────────────────────────────────────────────────────────┘

 TerminalMe > Inteligência Artificial
┌──────────────────────────────────────────────────────────────┐
│                                                              │

|  (1) Por onde começar a estudar inteligência artificial?
|      Criada por Ana Souza em 07/11/2026 10:20
|
|  (2) Inteligência no trabalho: como aplicar por meio da inteligência artificial?
|      Criada por Bruno Lima em 07/11/2026 10:20
|
|  (3) Como desenvolver inteligência emocional sendo gestor?
|      Criada por Ana Souza em 07/11/2026 10:20
|
|  Página 1 de 1
|
|  [A] Página anterior    [B] Próxima página    [R] Retornar
|  Ou digite o número da pergunta para abri-la
|
|  Para realizar outra busca pressione qualquer outra tecla
│                                                              │
└──────────────────────────────────────────────────────────────┘

 TerminalMe >
```

A ordem 1, 3, 2 é a mesma do enunciado. Com N = 4 perguntas, o termo "inteligencia" está em 3
listas e "artificial" em 2:

| Pergunta | inteligencia, IDF = log(4/3) = 0,125 | artificial, IDF = log(4/2) = 0,301 | TF×IDF |
|---|---|---|---|
| 1 | 0,333 × 0,125 = 0,042 | 0,333 × 0,301 = 0,100 | **0,142** |
| 3 | 0,4 × 0,125 = 0,050 | 0,2 × 0,301 = 0,060 | **0,110** |
| 2 | 0,333 × 0,125 = 0,042 | não tem o termo | **0,042** |
| 4 | não tem o termo | não tem o termo | fica fora do resultado |

**Antes da alteração das palavras-chave.** A busca por "artificial" encontra as perguntas 1 e 3:

```
 TerminalMe > artificial
┌──────────────────────────────────────────────────────────────┐
│                                                              │

|  (1) Por onde começar a estudar inteligência artificial?
|      Criada por Ana Souza em 07/11/2026 10:20
|
|  (2) Inteligência no trabalho: como aplicar por meio da inteligência artificial?
|      Criada por Bruno Lima em 07/11/2026 10:20
|
|  Página 1 de 1
```

**A alteração.** Em Minhas perguntas, Ana troca as palavras-chave da pergunta 1. A palavra "de" é
uma stop word e não entra no índice:

```
 TerminalMe > 2
 (1) 07/11/2026 10:20
 Por onde começar a estudar inteligência artificial?
 Palavras chave: introdução;inteligência;artificial

 (2) 07/11/2026 10:20
 Como desenvolver inteligência emocional sendo gestor?
 Palavras chave: inteligência;emocional;gestores

 Digite o número da pergunta (0 para cancelar):
 TerminalMe > 1
 Texto atual:
 Por onde começar a estudar inteligência artificial?
 Digite o novo texto (linha vazia para terminar, só Enter mantém o atual):
 TerminalMe >
 Palavras chave atuais: introdução;inteligência;artificial
 Digite as novas palavras chave separadas por ; (só Enter mantém as atuais):
 TerminalMe > aprendizado de máquina;python
Pergunta alterada com sucesso!
 Pressione Enter para continuar...
```

**Depois da alteração.** A mesma busca por "artificial" traz só a pergunta 3, e a pergunta 1 passa a
ser encontrada pelos termos novos:

```
 TerminalMe > artificial
┌──────────────────────────────────────────────────────────────┐
│                                                              │

|  (1) Inteligência no trabalho: como aplicar por meio da inteligência artificial?
|      Criada por Bruno Lima em 07/11/2026 10:20
|
|  Página 1 de 1
```

```
 TerminalMe > aprendizado máquina
┌──────────────────────────────────────────────────────────────┐
│                                                              │

|  (1) Por onde começar a estudar inteligência artificial?
|      Criada por Ana Souza em 07/11/2026 10:20
|
|  Página 1 de 1
```

As listas depois da alteração mostram que o ID 1 saiu de "introducao", "inteligencia" e
"artificial" e entrou nos três termos novos. O número de perguntas continua 4:

```
Entidades: 4

LISTAS INVERTIDAS:
introducao: (4;0.33333334)
inteligencia: (2;0.33333334) (3;0.4)
artificial: (3;0.2)
emocional: (2;0.33333334)
gestores: (2;0.33333334)
trabalho: (3;0.2)
meio: (3;0.2)
gestao: (4;0.33333334)
equipes: (4;0.33333334)
aprendizado: (1;0.33333334)
maquina: (1;0.33333334)
python: (1;0.33333334)
```

Uma busca sem nenhum resultado mostra "Nenhuma pergunta encontrada para esses termos.". Com mais de
dez resultados, as opções A e B trocam de página, e o rodapé mostra, por exemplo, "Página 2 de 2".

### Perguntas mais recentes

Cinco perguntas por página, da mais nova para a mais antiga, de qualquer usuário. A letra navega
entre as páginas e o número abre a pergunta.

```
┌────────────────────────────────────────────────────┐
│~Início > Buscar perguntas > Perguntas mais recentes│
└────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────┐
│                                                              │

|  (1) Por que a busca binária exige arquivo ordenado?
|      Criada por Carla Mendes em 18/10/2026 23:38
|
|  (2) Como ordenar um arquivo maior que a memória?
|      Criada por Bruno Lima em 18/10/2026 23:38
|
|  (3) Quando usar hash extensível em vez de árvore B+?
|      Criada por Bruno Lima em 18/10/2026 23:38
|
|  (4) Por que o registro guarda uma lápide?
|      Criada por Ana Souza em 18/10/2026 23:38
|
|  (5) Qual a diferença entre índice direto e indireto?
|      Criada por Ana Souza em 18/10/2026 23:38
|
|  Página 1 de 2
|
|  [A] Página anterior    [B] Próxima página    [R] Retornar
|  Ou digite o número da pergunta para abri-la
│                                                              │
└──────────────────────────────────────────────────────────────┘

 TerminalMe > B
```

```
┌────────────────────────────────────────────────────┐
│~Início > Buscar perguntas > Perguntas mais recentes│
└────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────┐
│                                                              │

|  (1) Como funciona a árvore B+ do relacionamento 1:N?
|      Criada por Ana Souza em 18/10/2026 23:38
|
|  Página 2 de 2
|
|  [A] Página anterior    [B] Próxima página    [R] Retornar
|  Ou digite o número da pergunta para abri-la
│                                                              │
└──────────────────────────────────────────────────────────────┘

 TerminalMe > A
```

### Pergunta com as respostas

As respostas aparecem em ordem decrescente de nota. A resposta arquivada não aparece.

```
┌───────────────────────────────────────────────────────────────┐
│~Início > Buscar perguntas > Perguntas mais recentes > Pergunta│
└───────────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────┐
│                                                              │

|  Por que o registro guarda uma lápide?
|  Pergunta criada por Ana Souza em 18/10/2026 23:38
|  Palavras chave: arquivo; lapide
|
|  RESPOSTAS
|  ---------
|  (1)
|  Para não mover os registros seguintes ao excluir.
|  Respondido por Bruno Lima em 18/10/2026 às 23:38
|  Nota: 2  (2 avaliações)
|
|  (2)
|  Para os índices continuarem apontando certo.
|  Respondido por Carla Mendes em 18/10/2026 às 23:38
|  Nota: 0  (2 avaliações)
|
|
|  [A] Responder à pergunta
|  [R] Retornar
|  Ou digite o número da resposta para abri-la
│                                                              │
└──────────────────────────────────────────────────────────────┘

 TerminalMe > 1
```

### Responder

A pergunta é mostrada de novo, e o usuário digita só o texto. Os demais atributos são preenchidos
pelo sistema. Uma linha vazia termina o texto.

```
┌───────────────────────────────────────────────────────────────────────────┐
│~Início > Buscar perguntas > Perguntas mais recentes > Pergunta > Responder│
└───────────────────────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────┐
│                                                              │

|  Por que o registro guarda uma lápide?
|  Pergunta criada por Ana Souza em 18/10/2026 23:38
│                                                              │
└──────────────────────────────────────────────────────────────┘

 Digite a resposta (linha vazia para terminar):
 TerminalMe > Para os índices continuarem apontando certo.
 TerminalMe >
Resposta publicada com sucesso!
 Pressione Enter para continuar...
```

### Resposta de outro usuário

Quem não é o autor da resposta pode concordar ou discordar. A nota sobe ou desce 1 ponto, e o total
de avaliações sobe 1.

```
┌──────────────────────────────────────────────────────────────────────────┐
│~Início > Buscar perguntas > Perguntas mais recentes > Pergunta > Resposta│
└──────────────────────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────┐
│                                                              │

|  Por que o registro guarda uma lápide?
|  Pergunta criada por Ana Souza em 18/10/2026 23:38
|
|  RESPOSTA
|  --------
|  Para não mover os registros seguintes ao excluir.
|  Respondido por Bruno Lima em 18/10/2026 às 23:38
|  Nota: 0  (0 avaliações)
|
|  [A] Concordar com a resposta
|  [B] Discordar da resposta
|
|  [R] Retornar ao menu anterior
│                                                              │
└──────────────────────────────────────────────────────────────┘

 TerminalMe > A
Você concordou com a resposta.
 Pressione Enter para continuar...
```

Uma segunda avaliação da mesma resposta, pelo mesmo usuário, é recusada com a mensagem "Você já
avaliou esta resposta".

### A própria resposta

O autor da resposta só tem a opção de arquivar. A resposta não pode ser alterada porque pode já ter
recebido avaliações.

```
┌──────────────────────────────────────────────────┐
│~Início > Minha Área > Minhas Respostas > Resposta│
└──────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────┐
│                                                              │

|  Por que o registro guarda uma lápide?
|  Pergunta criada por Ana Souza em 18/10/2026 23:38
|
|  RESPOSTA
|  --------
|  Para não mover os registros seguintes ao excluir.
|  Respondido por você em 18/10/2026 às 23:38
|  Nota: 2  (2 avaliações)
|
|  [A] Arquivar resposta
|
|  [R] Retornar ao menu anterior
│                                                              │
└──────────────────────────────────────────────────────────────┘

 TerminalMe > R
```

### Minha Área

```
┌────────────────────┐
│~Início > Minha Área│
└────────────────────┘
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│   [0]  Meus dados                                            │
│   [1]  Minhas perguntas                                      │
│   [2]  Minhas respostas                                      │
│   [3]  Minhas avaliações                                     │
│                                                              │
│   [4]  Retornar ao menu anterior                             │
│                                                              │
└──────────────────────────────────────────────────────────────┘

 TerminalMe > 1
```

### Minhas perguntas

A opção 4 abre uma pergunta do usuário com as respostas que ela recebeu. Alterar e arquivar a
pergunta continuam nas opções 2 e 3, como no TP1.

```
┌───────────────────────────────────────┐
│~Início > Minha Área > Minhas perguntas│
└───────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│   [0]  Listar                                                │
│   [1]  Incluir                                               │
│   [2]  Alterar                                               │
│   [3]  Arquivar                                              │
│   [4]  Visualizar pergunta e respostas                       │
│                                                              │
│   [5]  Retornar ao menu anterior                             │
│                                                              │
└──────────────────────────────────────────────────────────────┘

 TerminalMe > 4
 (1) 18/10/2026 23:38
 Como funciona a árvore B+ do relacionamento 1:N?
 Palavras chave: arvore b+; indice

 (2) 18/10/2026 23:38
 Qual a diferença entre índice direto e indireto?
 Palavras chave: indice; hash

 (3) 18/10/2026 23:38
 Por que o registro guarda uma lápide?
 Palavras chave: arquivo; lapide

 Digite o número da pergunta (0 para cancelar):
 TerminalMe > 3
```

### Resposta de outro usuário numa pergunta própria

O autor da pergunta ganha a opção C, para arquivar uma resposta que considere incompatível com a
pergunta.

```
┌─────────────────────────────────────────────────────────────┐
│~Início > Minha Área > Minhas perguntas > Pergunta > Resposta│
└─────────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────┐
│                                                              │

|  Por que o registro guarda uma lápide?
|  Pergunta criada por Ana Souza em 18/10/2026 23:38
|
|  RESPOSTA
|  --------
|  Para os índices continuarem apontando certo.
|  Respondido por Carla Mendes em 18/10/2026 às 23:38
|  Nota: 0  (2 avaliações)
|
|  [A] Concordar com a resposta
|  [B] Discordar da resposta
|  [C] Arquivar resposta (você é o autor da pergunta)
|
|  [R] Retornar ao menu anterior
│                                                              │
└──────────────────────────────────────────────────────────────┘

 TerminalMe > C
Arquivar é definitivo, a resposta sai das listagens. Tem certeza (s/n) ?
s
Resposta arquivada com sucesso!
 Pressione Enter para continuar...
```

De volta à pergunta, a resposta arquivada não aparece mais:

```
┌──────────────────────────────────────────────────┐
│~Início > Minha Área > Minhas perguntas > Pergunta│
└──────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────┐
│                                                              │

|  Por que o registro guarda uma lápide?
|  Pergunta criada por Ana Souza em 18/10/2026 23:38
|  Palavras chave: arquivo; lapide
|
|  RESPOSTAS
|  ---------
|  (1)
|  Para não mover os registros seguintes ao excluir.
|  Respondido por Bruno Lima em 18/10/2026 às 23:38
|  Nota: 1  (1 avaliações)
|
|
|  [A] Responder à pergunta
|  [R] Retornar
|  Ou digite o número da resposta para abri-la
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### Minhas respostas

Todas as respostas que o usuário escreveu, em qualquer pergunta. As arquivadas aparecem com a marca
`[ARQUIVADA]`. O número abre a tela "A própria resposta", mostrada acima.

```
┌───────────────────────────────────────┐
│~Início > Minha Área > Minhas Respostas│
└───────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────┐
│                                                              │

|  (1) Pergunta: Por que o registro guarda uma lápide?
|      Sua resposta: Para não mover os registros seguintes ao excluir.
|
|  Digite o número da resposta para abri-la, ou R para retornar
│                                                              │
└──────────────────────────────────────────────────────────────┘

 TerminalMe > 1
```

### Minhas avaliações

```
┌────────────────────────────────────────┐
│~Início > Minha Área > Minhas avaliações│
└────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────┐
│                                                              │

|  (1) POSITIVA - Para não mover os registros seguintes ao excluir.
|  (2) POSITIVA - Para os índices continuarem apontando certo.
|
|  Digite o número da avaliação para abri-la, ou R para retornar
│                                                              │
└──────────────────────────────────────────────────────────────┘

 TerminalMe > 1
```

A tela da avaliação mostra a pergunta, a resposta e o voto. A única opção é cancelar: não existe
trocar a avaliação, e para votar de novo o usuário encontra a resposta pela busca.

```
┌────────────────────────────────────────────────────┐
│~Início > Minha Área > Minhas avaliações > Avaliação│
└────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────┐
│                                                              │

|  Por que o registro guarda uma lápide?
|  Pergunta criada por Ana Souza em 18/10/2026 23:38
|
|  RESPOSTA
|  --------
|  Para não mover os registros seguintes ao excluir.
|  Respondido por Bruno Lima em 18/10/2026 às 23:38
|  Nota: 2  (2 avaliações)
|
|  Sua avaliação: POSITIVA (você concordou)
|
|  [A] Cancelar avaliação
|
|  [R] Retornar ao menu anterior
│                                                              │
└──────────────────────────────────────────────────────────────┘

 TerminalMe > A
Avaliação cancelada. A nota da resposta foi ajustada.
 Pressione Enter para continuar...
```

Depois do cancelamento, a resposta do Bruno passa de `Nota: 2  (2 avaliações)` para
`Nota: 1  (1 avaliações)`, e a avaliação sai da lista.

### Telas que vêm do TP1

#### Tela inicial

```
  ████████╗███████╗██████╗ ███╗   ███╗██╗███╗   ██╗ █████╗ ██╗     ███╗   ███╗███████╗
  ╚══██╔══╝██╔════╝██╔══██╗████╗ ████║██║████╗  ██║██╔══██╗██║     ████╗ ████║██╔════╝
     ██║   █████╗  ██████╔╝██╔████╔██║██║██╔██╗ ██║███████║██║     ██╔████╔██║█████╗
     ██║   ██╔══╝  ██╔══██╗██║╚██╔╝██║██║██║╚██╗██║██╔══██║██║     ██║╚██╔╝██║██╔══╝
     ██║   ███████╗██║  ██║██║ ╚═╝ ██║██║██║ ╚████║██║  ██║███████╗██║ ╚═╝ ██║███████╗
     ╚═╝   ╚══════╝╚═╝  ╚═╝╚═╝     ╚═╝╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝╚══════╝╚═╝     ╚═╝╚══════╝

                         Terminal Question & Answer System
                                Version 1.0.0

    ┌──────────────────────────────────────────────────────────────┐
    │                        MAIN MENU                             │
    ├──────────────────────────────────────────────────────────────┤
    │                                                              │
    │   [0]  LOGIN                                                 │
    │   [1]  CRIAR CONTA                                           │
    │   [2]  SAIR                                                  │
    │                                                              │
    └──────────────────────────────────────────────────────────────┘

 TerminalMe > 1
```

#### Criar conta

```
Deseja continuar (S/N) : S
 Digite seu email:
 TerminalMe > ana@exemplo.com
 Digite seu nome:
 TerminalMe > Ana Souza
 Digite sua senha:
 TerminalMe > senha123
 Digite sua pergunta secreta:
 TerminalMe > Nome do primeiro pet
 Digite sua resposta secreta
 TerminalMe > Rex
```

#### Login

```
Deseja continuar (S/N) : S
 Digite seu email:
 TerminalMe > ana@exemplo.com
 Digite sua senha:
 TerminalMe > senha123
```

#### Login com senha errada

```
Deseja continuar (S/N) : S
 Digite seu email:
 TerminalMe > ana@exemplo.com
 Digite sua senha:
 TerminalMe > senha errada
Email ou senha incorretos!
   [0]  Tentar novamente
   [1]  Recuperar senha
   [2]  Retornar ao menu inicial
 TerminalMe > 1
Nome do primeiro pet ?
rex
 Digite a nova senha:
 TerminalMe > outrasenha
```

A resposta secreta é aceita com qualquer acentuação e qualquer caixa, porque o hash é gerado a
partir do texto já normalizado. São três tentativas; esgotadas, o sistema volta ao menu inicial.

#### Meus dados

```
┌─────────────────────────────────┐
│~Início > Minha Área > Meus dados│
└─────────────────────────────────┘
┌──────────────────────────────────────────────────────────────┐
│                                                              │
 Nome : Ana Souza
 Email : ana@exemplo.com
 Pergunta : Nome do primeiro pet
│                                                              │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                                                              │
│   [0]  Alterar nome                                          │
│   [1]  Alterar email                                         │
│   [2]  Alterar senha                                         │
│   [3]  Alterar pergunta e resposta de recuperação da senha   │
│   [4]  Apagar Conta                                          │
│                                                              │
│   [5]  Retornar ao menu anterior                             │
│                                                              │
└──────────────────────────────────────────────────────────────┘

 TerminalMe > 5
```

#### Incluir pergunta

O texto pode ter várias linhas, e uma linha vazia termina o texto.

```
 TerminalMe > 1
 Digite a pergunta (linha vazia para terminar):
 TerminalMe > Como funciona a árvore B+ usada no relacionamento 1:N?
 TerminalMe > Ela aceita chaves repetidas?
 TerminalMe >
 Digite as palavras chave separadas por ; :
 TerminalMe > arvore b+; indice; 1:N
Pergunta incluída com sucesso!
 Pressione Enter para continuar...
```

#### Alterar pergunta

A lista numera as perguntas sem mostrar o ID. Enter vazio mantém o valor atual.

```
 TerminalMe > 2
 (1) 19/09/2026 14:12
 Como funciona a árvore B+ usada no relacionamento 1:N?
 Ela aceita chaves repetidas?
 Palavras chave: arvore b+; indice; 1:N

 (2) 19/09/2026 14:12
 Qual a diferença entre índice direto e indireto?
 Palavras chave: indice; hash

 (3) 19/09/2026 14:12
 Por que o registro guarda uma lápide?
 Palavras chave: arquivo; lapide

 Digite o número da pergunta (0 para cancelar):
 TerminalMe > 2
 Texto atual:
 Qual a diferença entre índice direto e indireto?
 Digite o novo texto (linha vazia para terminar, só Enter mantém o atual):
 TerminalMe >
 Palavras chave atuais: indice; hash
 Digite as novas palavras chave separadas por ; (só Enter mantém as atuais):
 TerminalMe > indice direto; indice indireto; hash extensivel
Pergunta alterada com sucesso!
 Pressione Enter para continuar...
```

#### Arquivar pergunta

```
 TerminalMe > 3
 (1) 19/09/2026 14:12
 Como funciona a árvore B+ usada no relacionamento 1:N?
 Ela aceita chaves repetidas?
 Palavras chave: arvore b+; indice; 1:N

 (2) 19/09/2026 14:12
 Qual a diferença entre índice direto e indireto?
 Palavras chave: indice direto; indice indireto; hash extensivel

 (3) 19/09/2026 14:12
 Por que o registro guarda uma lápide?
 Palavras chave: arquivo; lapide

 Digite o número da pergunta (0 para cancelar):
 TerminalMe > 3
Arquivar é definitivo, a pergunta não poderá mais ser alterada. Tem certeza (s/n) ?
s
Pergunta arquivada com sucesso!
 Pressione Enter para continuar...
```

#### Listar perguntas

O estado só aparece quando a pergunta está arquivada.

```
 TerminalMe > 0
 (1) 19/09/2026 14:12
 Como funciona a árvore B+ usada no relacionamento 1:N?
 Ela aceita chaves repetidas?
 Palavras chave: arvore b+; indice; 1:N

 (2) 19/09/2026 14:12
 Qual a diferença entre índice direto e indireto?
 Palavras chave: indice direto; indice indireto; hash extensivel

 (3) ARQUIVADA 19/09/2026 14:12
 Por que o registro guarda uma lápide?
 Palavras chave: arquivo; lapide

 Pressione Enter para continuar...
```

## Classes e métodos

### Classes criadas pelo grupo

**`Main`**
- `main(String[])`: cria o `ArquivoUser`, o `ArquivoPergunta`, o `ArquivoResposta`, o `ArquivoAvaliacao`, o `ManagerEntities`, o `ContextView` com a `StateInicial` e o `Controller`, que inicia as telas.

**`Security.Crypt`**
- `getSHA256(String)`: devolve o hash SHA-256 do texto em hexadecimal. É usado na senha.

**`Formatter.FormatterString`**
- `converteNLines(String)`: quebra o texto em linhas com o prefixo `|  `, para caber nas caixas das telas. É usado pelo `showMode` da pergunta e da resposta.

**`control.Controller`**, as regras de negócio:

| Método | O que faz |
|---|---|
| `addUser(User)` | valida o email e grava o usuário |
| `updateUser(User)` | valida o email e regrava o usuário |
| `updateSenhaUser(String email, String hashSenha)` | troca a senha na recuperação |
| `deleteUser(int id)` | apaga o usuário em cascata: desfaz e apaga as avaliações que ele deu, apaga as respostas que ele escreveu, apaga as perguntas dele com todas as respostas que elas receberam e, por último, o usuário |
| `apagarResposta(int idResposta)` (privado) | apaga as avaliações que a resposta recebeu e depois a resposta |
| `findUser(String email)` | diz se já existe usuário com o email |
| `readUser(String email, String hashSenha)` | faz o login, conferindo email e senha |
| `recoveryQuestion(String email)` | devolve a pergunta secreta e o hash da resposta |
| `normalizeResponse(String)` | tira os espaços das pontas, remove os acentos e passa para minúsculas |
| `hashRespostaSecreta(String)` | normaliza a resposta e devolve o hash dela; é o único ponto do sistema que gera esse valor |
| `nomeUsuario(int id)` | devolve o nome do usuário, mostrado como autor nas telas |
| `listarMinhasPerguntas()` | devolve as perguntas do usuário logado, pela Árvore B+ do relacionamento 1:N |
| `addPergunta(String texto, String palavras)` | valida (texto e palavras não vazios, até 4.000 e 1.000 caracteres, usuário existente) e grava a pergunta ligada ao usuário logado |
| `updatePergunta(Pergunta, String texto, String palavras)` | confere o dono e se a pergunta está ativa, mantém o campo deixado em branco, atualiza a data de alteração e regrava |
| `arquivarPergunta(Pergunta)` | confere o dono e se a pergunta ainda está ativa, marca como arquivada e regrava |
| `readPergunta(int id)` | lê uma pergunta pelo ID |
| `readPerguntasByWords(String termos)` (TP3) | repassa a consulta ao `ArquivoPergunta` e devolve as perguntas ativas em ordem decrescente de TF×IDF |
| `listarPerguntasRecentes(int pagina)` | devolve a página pedida da listagem por data, com `PERGUNTAS_POR_PAGINA = 5` |
| `totalPaginasRecentes()` | devolve o número de páginas, com a divisão arredondada para cima |
| `readRespostasByPergunta(int idPergunta)` | devolve as respostas ativas da pergunta, ordenadas por nota decrescente |
| `readRespostasByUser(int idUsuario)` | devolve as respostas escritas pelo usuário, inclusive as arquivadas |
| `readResposta(int id)` | lê uma resposta pelo ID |
| `addResposta(int idPergunta, String texto)` | recusa texto vazio ou com mais de 4.000 caracteres, confere que o usuário e a pergunta existem e que a pergunta está ativa, e grava a resposta com nota 0 |
| `arquivarResposta(Resposta)` | aceita só o autor da resposta ou o autor da pergunta, recusa resposta já arquivada e marca como arquivada sem mexer na data de criação |
| `avaliarResposta(Resposta, boolean positiva)` | recusa a própria resposta, resposta arquivada e segunda avaliação; grava a avaliação e ajusta a nota e o total de avaliações da resposta |
| `existAvalaicaoUserResponse(int idUser, int idResponse)` | procura, nas avaliações do usuário, uma avaliação daquela resposta |
| `listarMinhasAvaliacoes()` | devolve as avaliações do usuário logado |
| `cancelarAvaliacao(Avaliacao)` | confere que a avaliação é do usuário logado e a desfaz |
| `desfazerAvaliacao(Avaliacao)` (privado) | tira 1 ponto da nota se a avaliação era positiva, ou devolve 1 ponto se era negativa, diminui o total de avaliações e apaga a avaliação |

**`control.SingletonUser`**
- `setUser(User)` e `getInstanceUser()`: guardam e devolvem o usuário logado.

**`model.Registro`** (interface)
- `setId`, `getId`, `serialize` e `deserialize`: o contrato que o `Arquivo<T>` exige das entidades.

**`model.User`**
- os construtores, os getters e setters dos atributos, e `serialize()` e `deserialize(byte[])`.

**`model.Pergunta`**
- `Pergunta(int idUsuario, String pergunta, String palavrasChave)`: preenche a criação e a alteração com o momento atual, a nota com 0 e `ativa` com `true`;
- os getters, o `setId` exigido pelo `Arquivo`, e os setters do texto, das palavras-chave e da
  data de alteração. Não há setter para `idUsuario`, `criacao` nem `ativa`;
- `arquivar()`: põe `ativa = false`. Não existe o caminho de volta;
- `getCriacaoFormatada()` e `getAlteracaoFormatada()`: a data no formato `dd/MM/yyyy HH:mm`;
- `showMode(String nomeAutor)`: o texto da pergunta formatado para as telas, com o autor e a data;
- `serialize()` e `deserialize(byte[])`.

**`model.Resposta`**
- os construtores, os getters e setters dos atributos e o `setId` exigido pelo `Arquivo`;
- `arquivar()`: põe `ativa = false`;
- `getCriacaoFormatada()`: a data no formato `dd/MM/yyyy às HH:mm`;
- `showMode(String nomeAutor)`: o texto da resposta formatado para as telas, com o autor, a data, a nota e o total de avaliações, e a marca `[ARQUIVADA]` quando for o caso;
- `serialize()` e `deserialize(byte[])`.

**`model.Avaliacao`**
- os construtores, os getters, `getItSelfAvalaiacao()` e `setItSelfAvaliacao(boolean)`, o `setId` exigido pelo `Arquivo`, e `serialize()` e `deserialize(byte[])`.

**`model.ManagerEntities`**
- `getArqUsers()`, `getArqPergunta()`, `getArqResposta()` e `getArqAvalaiacao()`: dão ao controle o acesso aos quatro arquivos.

**`model.AcessDataBase.ArquivoUser`** (estende `Arquivo<User>`)

| Método | O que faz |
|---|---|
| `create(User)` | recusa email repetido, confere se o email cabe no par de 64 bytes, e só então grava o registro e inclui o par (email; id) na hash extensível |
| `read(String email)` | busca o usuário pelo índice indireto de email |
| `update(User)` | quando o email muda, confere que o novo está livre, monta o par novo, inclui no índice e só então apaga o antigo; o registro é regravado depois, então uma falha no índice não deixa a conta inacessível |
| `delete(int id)` e `delete(String email)` | apagam o usuário e o par do índice de email |

**`model.AcessDataBase.ArquivoPergunta`** (estende `Arquivo<Pergunta>`)

| Método | O que faz |
|---|---|
| construtor | abre as duas árvores e a `ListaInvertida` (30 pares por bloco); se a árvore por data estiver vazia, chama `reconstruirIndiceData()`, e se o índice invertido estiver vazio, chama `reconstruirIndiceInvertido()` |
| `reconstruirIndiceData()` (privado) | percorre o arquivo uma vez e indexa por data as perguntas ativas que já existiam antes do índice |
| `STOP_WORDS` (TP3) | o conjunto das stop words, já sem acento |
| `extrairTermos(String)` (TP3, privado) | monta o vetor de termos: tira os acentos, passa para minúsculas, separa o texto em termos e descarta as stop words. Serve tanto às palavras-chave quanto à consulta |
| `createRefInvert(Pergunta)` (TP3, privado) | calcula o TF de cada termo (1 dividido pelo total de termos) e grava o par (idPergunta; TF) na lista do termo; se o termo se repete, lê o par com `read(termo, id)` e soma a frequência com `update` |
| `removeAllKeyWords(Pergunta)` (TP3) | tira o ID da pergunta da lista de cada termo das palavras-chave dela |
| `reconstruirIndiceInvertido()` (TP3, privado) | indexa as perguntas que já existiam no arquivo quando o índice invertido ainda não existia, como as dos dados do TP2 |
| `create(Pergunta)` | grava a pergunta, inclui o par (idUsuario; idPergunta) na árvore do relacionamento e o par (criação; idPergunta) na árvore por data; no TP3, também soma 1 ao número de perguntas do índice e indexa as palavras-chave |
| `readPergunta(int id)` | lê a pergunta pelo índice direto |
| `readByUser(int idUsuario)` | busca na árvore do relacionamento todos os pares do usuário e lê cada pergunta pelo índice direto |
| `readRecentes(int pagina, int porPagina)` | lê todos os pares da árvore por data, já em ordem decrescente, e lê do arquivo só as perguntas da página pedida |
| `contarRecentes()` | devolve quantas perguntas ativas estão na árvore por data |
| `readPerguntasByWords(String consulta)` (TP3) | a busca: monta os termos da consulta, soma TF×IDF por pergunta e devolve as perguntas ativas em ordem decrescente de valor |
| `update(Pergunta)` | regrava a pergunta e, se ela foi arquivada, tira o par da árvore por data; no TP3, lê antes a versão gravada, tira do índice as palavras-chave antigas e indexa as novas |
| `delete(int id)` | remove o par das duas árvores e depois o registro; no TP3, também tira a pergunta do índice invertido e diminui em 1 o número de perguntas |

O ID, o `idUsuario` e a data de criação de uma pergunta nunca mudam, então os pares das duas árvores
e do índice invertido continuam válidos mesmo quando o registro muda de lugar no arquivo.

**`model.AcessDataBase.ParDataId`**
- é o par (data de criação; idPergunta) da árvore por data, com tamanho fixo de 12 bytes (`long` mais `int`). O `compareTo` é invertido de propósito, para a data maior vir antes, e desempata pelo ID quando duas perguntas têm o mesmo milissegundo.

**`model.AcessDataBase.ArquivoResposta`** (estende `Arquivo<Resposta>`)

| Método | O que faz |
|---|---|
| `create(Resposta)` | grava a resposta e inclui os pares (idUsuario; idResposta) e (idPergunta; idResposta) nas duas árvores |
| `delete(int id)` | remove o par das duas árvores e depois o registro |
| `readByUser(int idUsuario)` | devolve as respostas do usuário, inclusive as arquivadas |
| `readByPergunta(int idPergunta)` | devolve só as respostas ativas da pergunta |
| `readTodasByPergunta(int idPergunta)` | devolve todas as respostas da pergunta, inclusive as arquivadas; é usado na exclusão em cascata |
| `incrementNota`, `decrementNota`, `incrementAvaliacao` e `decrementAvaliacao` | somam ou tiram 1 da nota ou do total de avaliações da resposta e regravam o registro |

**`model.AcessDataBase.ArquivoAvaliacao`** (estende `Arquivo<Avaliacao>`)

| Método | O que faz |
|---|---|
| `create(Avaliacao)` | grava a avaliação e inclui os pares (idUsuario; idAvaliacao) e (idResposta; idAvaliacao) nas duas árvores |
| `delete(int id)` | remove o par das duas árvores e depois o registro |
| `readByUser(int idUsuario)` | devolve as avaliações feitas pelo usuário |
| `readByResposta(int idResposta)` | devolve as avaliações que a resposta recebeu |

**`model.AcessDataBase.ParIDEMAIL`**
- é o par (email; id) da hash extensível, com tamanho fixo de 64 bytes. A chave é `Math.abs(email.hashCode())`, e emails acima de 60 bytes são recusados.

**Pacote `view`:**

| Classe | Papel |
|---|---|
| `ContextView` | `start()` e `render()` desenham a tela atual; `changeState(StateView)` define a próxima; `reportFail(Exception)` limpa a tela, mostra a mensagem do erro e espera um segundo antes de redesenhar |
| `StateView` | classe base das telas: `draw()` (abstrato), `clearScreen()`, `lerOpcao()` (lê a opção do menu sem derrubar o programa se vier uma letra), `lerTexto()` (várias linhas), `pausar()`, `paraNumero()` (converte o texto digitado em número, ou -1), `avisarEmConstrucao()` e `getTheSameScan()`, que cria o `Scanner` único na codificação do console |
| `StateInicial` | menu de acesso: login, criar conta e sair |
| `StateCreateAccount` | cadastro de usuário; um email já cadastrado devolve a própria tela, para o usuário informar outro |
| `StateLogin` | login, mensagem de erro com as opções de tentar novamente, recuperar senha ou retornar, e a recuperação pela pergunta secreta em até três tentativas |
| `StateMain` | menu do usuário logado: Minha Área e Buscar perguntas |
| `StateSearchQuestions` | menu da busca: perguntas mais recentes e busca por palavras-chave |
| `StateByKeyWordQuestions` (TP3) | busca por palavras-chave: lê os termos, mostra o resultado de 10 em 10 com A e B para trocar de página, abre a pergunta pelo número e, no "Retornar" da pergunta, volta para a mesma página do resultado sem refazer a busca |
| `StateRecentQuestions` | listagem paginada das perguntas mais recentes; guarda a página atual, navega com A e B e abre a pergunta pelo número |
| `StateQuestion` | a pergunta completa com as respostas ativas em ordem de nota; abre a resposta pelo número e leva a Responder; é usada pela busca e por Minhas perguntas |
| `StateResponse` | mostra a pergunta de novo, lê o texto da resposta e grava |
| `StateShowResponse` | a pergunta e uma resposta; o menu depende de quem vê: o autor da resposta só arquiva, os outros concordam ou discordam, e o autor da pergunta também pode arquivar |
| `StateAreaUser` | Minha Área |
| `StateDataUser` | Meus dados: `getDataUser()` monta o quadro com os dados, e `draw()` trata a alteração e a exclusão da conta. Na troca de email, monta um `User` novo e só substitui o usuário do `SingletonUser` depois que a gravação dá certo |
| `StateQuestionsUser` | Minhas perguntas: `listar()`, `incluir()`, `alterar()`, `arquivar()` e `visualizar()`, que abre a pergunta com as respostas, com os auxiliares `mostrarLista()`, `escolherPergunta()` e `lerNumero()` |
| `StateMyResponses` | Minhas respostas: lista as respostas do usuário e abre cada uma na `StateShowResponse` |
| `StateMyRatings` | Minhas avaliações: lista as avaliações do usuário, com o voto e o início da resposta |
| `StateShowRating` | uma avaliação: mostra a pergunta, a resposta e o voto, com a opção de cancelar |
| `SingletonPWD` | `push(String)`, `pop()`, `getPwd()` e `ultimaPagina(String)`: o caminho de navegação |
| `Layouts` e `Color` | o desenho dos menus e as cores do terminal; no TP3, o `Layouts` ganhou a caixa `SearchByKeyword` da busca |

### Classes do professor usadas sem alteração de lógica

- **`Arquivo<T>`:** o CRUD genérico com lápide, índice direto e reaproveitamento de espaço. Os métodos são `create`, `read`, `readAll`, `update`, `delete`, `insertFreeSlot`, `findFreeSlot`, `reorganizar` e `close`.
- **`HashExtensivel<T>`**, com a interface `RegistroHashExtensivel<T>` e o par `ParIDEndereco`.
- **`ListaInvertida`** (TP3), com o par **`ElementoLista`** (id; frequência). Os métodos usados são `incrementaEntidades`, `decrementaEntidades`, `numeroEntidades`, `create`, `read(termo)`, `read(termo, id)`, `update` e `delete`. O `print()` foi usado nos testes, para conferir as listas.

### Classe do professor com uma correção

- **`ArvoreBMais<T>`**, com a interface `InterfaceArvoreBMais<T>` e o par `ParIdId`. No TP3, o
  `delete` foi corrigido: a rotação com o irmão esquerdo deixava uma página interna inválida, o que
  derrubava o arquivamento e a exclusão de perguntas quando a árvore tinha mais de dois níveis. O
  item 8 de [Do TP3](#do-tp3) descreve o defeito e o teste.

## Operações especiais

### Do TP3

1. **Índice invertido com a `ListaInvertida`.** O `ArquivoPergunta` abre uma `ListaInvertida` com
   blocos de 30 pares, gravada em `DictionaryQuestions.db` e `InvertedQuestions.db`. O cabeçalho do
   dicionário guarda o número de perguntas indexadas, que é o N do IDF: cada inclusão chama
   `incrementaEntidades()` e cada exclusão chama `decrementaEntidades()`. O tamanho de cada lista é
   o número de perguntas que têm o termo. As listas guardam só o TF; o IDF é calculado na hora da
   busca, com esses dois números.
2. **Vetor de termos.** O `extrairTermos` faz o tratamento do enunciado: tira os acentos (forma NFD,
   sem as marcas), passa para minúsculas e descarta as stop words. Ele separa o texto em tudo que não
   é letra, número, `+` ou `#`. Assim o `;` entre as palavras-chave e o espaço dentro delas separam
   termos: `banco de dados;sql` vira `[banco, dados, sql]`, e cada termo vale 1/3. O `+` e o `#`
   ficam porque neste sistema de perguntas eles fazem parte de termos como `c++`, `c#` e `b+`. A
   consulta passa pelo mesmo método, então "Árvore" encontra a palavra-chave "arvore" e "banco de
   dados" encontra a pergunta acima.
3. **Stop words do domínio.** A lista tem artigos, preposições e suas contrações, conjunções,
   pronomes, numerais por extenso e alguns verbos auxiliares, todos sem acento porque são
   comparados depois da normalização. Para o domínio de perguntas e respostas, entraram também
   "duvida", "ajuda", "pergunta" e "alguem", que aparecem em qualquer pergunta e não dizem o
   assunto dela.
4. **Termo repetido soma a frequência.** O `createRefInvert` segue o esquema do enunciado: para cada
   termo, `read(termo, id)` procura o par da pergunta; se não existe, `create` grava (id; 1/n); se
   existe, `update` grava a frequência somada. Na pergunta 3 das capturas, "inteligencia" aparece duas
   vezes em cinco termos e fica com 0,2 + 0,2 = 0,4.
5. **Busca ordenada por TF×IDF.** O `readPerguntasByWords` monta os termos da consulta (termo repetido
   conta uma vez), lê a lista de cada um e calcula `IDF = log10(N / tamanho da lista)`. Cada par
   (id; TF) da lista soma `TF × IDF` ao valor daquela pergunta, num `HashMap<Integer, Double>`. No
   fim, os IDs são ordenados pelo valor, do maior para o menor, e cada pergunta é lida pelo índice
   direto. As perguntas arquivadas são lidas mas não entram no resultado. A tela mostra de 10 em
   10, como pede o enunciado. Perguntas com o mesmo valor não têm ordem definida entre si.
6. **O índice acompanha inclusão, alteração e exclusão.** Na inclusão, a pergunta entra no índice e N
   aumenta. Na alteração, o `Controller` troca as palavras-chave no próprio objeto antes de chamar o
   `update`, então as palavras antigas só existem no registro gravado. Por isso o `update` lê a
   versão gravada antes de regravar, tira o ID das listas dos termos antigos e indexa os novos. N não
   muda, porque a pergunta continua no arquivo. Na exclusão, que acontece quando o usuário apaga a
   conta, a pergunta sai das listas e N diminui. A `ListaInvertida` do professor não apaga o termo do
   dicionário quando a lista fica vazia; a busca ignora lista vazia. A pergunta arquivada continua no
   índice e em N, como continua no arquivo, e é filtrada na hora de montar o resultado.
7. **Reconstrução do índice.** Se o arquivo de perguntas já tem registros quando o índice invertido
   ainda está vazio, como acontece ao abrir os dados do TP2, o construtor do `ArquivoPergunta` indexa
   todas as perguntas uma vez. No teste, os dois arquivos do índice foram apagados com 16 perguntas
   gravadas: na abertura seguinte, N voltou a 16 e as buscas trouxeram o mesmo resultado de antes.
8. **Correção na `ArvoreBMais`.** Arquivar uma pergunta com 16 perguntas cadastradas mostrava
   "Index 3 out of bounds for length 3". O erro vinha do `delete` da árvore do professor, chamado
   para tirar a pergunta da árvore por data. Quando uma página interna fica com poucos elementos e
   pega emprestado do irmão esquerdo, o código descia o elemento do pai para a página, mas o irmão
   perdia só o último ponteiro, sem perder o último elemento. A página do irmão ficava com tantos
   ponteiros quanto elementos, e a gravação dela estourava no meio do `delete`, com parte das páginas
   já regravada. Agora o último elemento do irmão sobe para o pai, como numa rotação de Árvore B+.
   Um teste de estresse inseriu de 5 a 154 pares em 300 árvores de ordem 5 e apagou todos em ordem
   aleatória, conferindo depois de cada exclusão o conteúdo e a ordem da árvore. Com o código original,
   264 das 300 rodadas falharam; com a correção, nenhuma.
9. **Tela de busca.** A `StateByKeyWordQuestions` guarda o resultado da última busca, então voltar de
   uma pergunta reabre a mesma página sem refazer o cálculo. O número digitado só abre uma pergunta da
   página atual, A na primeira página e B na última mantêm a página, o rodapé mostra "Página X de Y",
   e uma busca sem resultado mostra "Nenhuma pergunta encontrada para esses termos.".

### Do TP2

1. **Índice de perguntas por data, em ordem decrescente.** A árvore `indiceData` guarda pares
   `ParDataId` (criação; idPergunta). A Árvore B+ mantém os elementos do menor para o maior segundo o
   `compareTo`, então o `compareTo` do `ParDataId` compara as datas ao contrário: a pergunta mais
   nova vira a "menor" chave e fica na primeira folha. A comparação usa `Long.compare`, e não a
   subtração das datas, porque `(int)(data1 - data2)` estoura quando a diferença passa de 2³¹
   milissegundos (cerca de 24 dias) e o sinal se inverte. Quando duas perguntas são criadas no mesmo
   milissegundo, o desempate é pelo ID, também decrescente; sem ele, a árvore trataria as duas como a
   mesma chave.
2. **Listagem paginada com `read(null)`.** No `read1` da árvore do professor, `null` significa "sem
   filtro": a busca desce até a folha mais à esquerda e segue os ponteiros `proxima` de folha em
   folha, juntando todos os pares, já na ordem decrescente. O `readRecentes` usa essa lista só para
   escolher os IDs da página: a página 0 pega as posições 0 a 4, a página 1 pega de 5 a 9, e assim por
   diante. Do arquivo de dados são lidas apenas as cinco perguntas da página; o restante é lido só
   como chave do índice, de 12 bytes cada. O número de páginas é `(total + 4) / 5`, a divisão
   arredondada para cima.
3. **Só perguntas ativas na busca.** A árvore por data recebe o par no `create` e perde o par quando a
   pergunta é arquivada (no `update` sobrescrito do `ArquivoPergunta`) ou apagada. Assim a pergunta
   arquivada sai da busca pública e deixa de aceitar respostas, e a paginação conta só o que pode ser
   mostrado. Se a árvore por data estiver vazia ao abrir o sistema, o construtor indexa uma vez as
   perguntas ativas que já existiam no arquivo, o que permitiu reaproveitar os dados do TP1.
4. **Relacionamento N:N entre usuários e perguntas.** A entidade `Resposta` fica entre as duas e
   guarda os dois IDs. O relacionamento é implementado como dois 1:N, cada um com a sua Árvore B+:
   (idPergunta; idResposta), usada para listar as respostas de uma pergunta, e (idUsuario;
   idResposta), usada em Minhas respostas. A busca por todos os pares de uma chave usa o mesmo truque
   do TP1: `new ParIdId(id, -1)`, em que o `-1` faz o `compareTo` considerar só a primeira chave. A
   avaliação segue o mesmo desenho, com as árvores (idUsuario; idAvaliacao) e (idResposta;
   idAvaliacao).
5. **Respostas ordenadas por nota.** O `readRespostasByPergunta` ordena a lista com
   `respostas.sort((a, b) -> b.getNota() - a.getNota())`. O comparador devolve negativo quando `a`
   deve vir antes de `b`, então `b - a` põe a nota maior primeiro. O `sort` do Java é estável:
   respostas com a mesma nota mantêm a ordem da árvore, com a mais antiga primeiro.
6. **Avaliação com três regras.** O `avaliarResposta` recusa a avaliação da própria resposta, de
   resposta arquivada e uma segunda avaliação do mesmo usuário na mesma resposta. Essa última regra é
   conferida pelas avaliações do usuário, lidas pela árvore (idUsuario; idAvaliacao). Só depois das
   três conferências a avaliação é gravada e a resposta recebe +1 avaliação e +1 ou -1 na nota.
7. **Cancelamento de avaliação.** O `desfazerAvaliacao` aplica o inverso do voto: se a avaliação era
   positiva, tira 1 ponto da nota; se era negativa, devolve 1 ponto; nos dois casos, o total de
   avaliações diminui 1, e a avaliação é apagada. As funções que ajustam a nota não conferem se a
   resposta está ativa, de propósito: cancelar uma avaliação numa resposta que foi arquivada depois
   do voto também precisa corrigir a nota. Quem impede avaliar resposta arquivada é o Controller.
8. **Arquivamento de respostas.** A resposta pode ser arquivada pelo autor dela ou pelo autor da
   pergunta, que pode retirar uma resposta incompatível com a pergunta. A data de criação não muda no
   arquivamento. A resposta arquivada some da tela da pergunta e continua em Minhas respostas, com a
   marca `[ARQUIVADA]`.
9. **Menu da resposta conforme quem vê.** A `StateShowResponse` compara o usuário logado com o autor
   da resposta e com o autor da pergunta: o autor da resposta só vê "Arquivar"; os demais veem
   "Concordar" e "Discordar"; o autor da pergunta vê também "Arquivar".
10. **Exclusão em cascata em quatro passos.** Ao apagar a conta, o `deleteUser` (1) desfaz as
    avaliações que o usuário deu, para as respostas de outras pessoas não continuarem contando o voto
    de quem saiu; (2) apaga as respostas que ele escreveu; (3) apaga as perguntas dele, cada uma
    depois das respostas que recebeu, inclusive as arquivadas; (4) apaga o usuário. Toda resposta
    apagada leva antes as avaliações que recebeu (`apagarResposta`). Os filhos saem sempre antes do
    pai, então uma falha no meio nunca deixa resposta sem pergunta nem avaliação sem resposta.
    Arquivar, ao contrário, não é cascata: arquivar uma pergunta não arquiva as respostas dela, e
    arquivar uma resposta não apaga as avaliações.
11. **Navegação entre telas.** A pergunta e a resposta podem ser abertas da busca ou da Minha Área,
    então recebem um `Supplier<StateView>` com a tela de volta, como em
    `() -> new StateRecentQuestions(pagina)`. A tela de volta só é criada no momento do "Retornar",
    porque o construtor de toda tela limpa o terminal e imprime o caminho de navegação. O caminho usa
    `ultimaPagina(String)`, que compara só o último trecho: o `contains` usado antes dava falso
    positivo, porque "Perguntas mais recentes" contém "Pergunta" e "Minhas Respostas" contém
    "Resposta".
12. **Respostas com várias linhas.** O texto da resposta vai até 4.000 caracteres, pelo mesmo motivo
    do limite da pergunta (o tamanho do registro é gravado num `short`), e pode ter várias linhas,
    terminadas por uma linha vazia. Resposta vazia ou só com espaços é recusada.

### Do TP1

1. **Índice indireto por email.** O login e a busca por email usam a Tabela Hash Extensível de
   pares (email; id), e não uma leitura sequencial do arquivo.
2. **Relacionamento 1:N com Árvore B+.** Cada pergunta nova grava o par
   (idUsuario; idPergunta) na árvore, de ordem 5. Para listar as perguntas de um usuário, o
   `readByUser` busca `new ParIdId(idUsuario, -1)`. No `compareTo` do `ParIdId`, o `-1` faz a
   comparação considerar só o `idUsuario`, então a árvore devolve todos os pares desse usuário, em
   ordem de ID. O caminho inverso, da pergunta para o usuário, é o próprio atributo `idUsuario`.
3. **Chave estrangeira.** A pergunta sempre recebe o ID do usuário logado, e o `addPergunta`
   confere que ele existe antes de gravar.
4. **Arquivamento definitivo.** Não existe exclusão de pergunta: arquivar é pôr `ativa = false`.
   Não existe desarquivar, a pergunta arquivada não pode ser alterada nem arquivada de novo, e na
   listagem do autor ela aparece como `ARQUIVADA`. O arquivamento também atualiza a data de
   alteração.
5. **Número da tela ligado ao ID real.** As listas numeram os itens (1), (2)..., sem ID. O número
   escolhido menos 1 é a posição na mesma `ArrayList` que foi mostrada, e cada elemento dessa lista
   guarda o ID real.
6. **Alteração que muda o tamanho do registro.** Se o texto novo cabe no espaço antigo, o registro
   é regravado no mesmo lugar. Se não cabe, o antigo recebe a lápide e vai para a lista de espaços
   livres, o novo é gravado em outro lugar, e só o índice direto é atualizado.
7. **Limites de tamanho.** O tamanho do registro é gravado num `short` e não pode passar de
   65.535 bytes. Por isso o texto da pergunta vai até 4.000 caracteres e as palavras-chave até
   1.000. No pior caso, com caracteres de 3 bytes, o registro fica em cerca de 15.000 bytes.
8. **Menus que não caem.** A opção é lida como texto. Uma letra ou um número inválido redesenha o
   menu, em vez de lançar exceção ou encerrar o programa.
9. **Resposta secreta normalizada antes do hash.** O `Controller.hashRespostaSecreta` passa a
   resposta pelo `normalizeResponse`, que tira os espaços das pontas, decompõe os caracteres em
   NFD, remove os acentos e passa tudo para minúsculas. Quem cadastra "Réx" recupera a senha
   digitando "rex". O cadastro, a alteração em Meus dados e a conferência na recuperação chamam
   esse mesmo método, então os três sempre concordam. A pergunta secreta, ao contrário, é gravada
   exatamente como foi digitada, porque ela é mostrada na tela.
10. **Índice validado antes da gravação.** O arquivo de dados e o índice de email precisam contar
    a mesma história. Por isso, tanto no `create` quanto no `update` do `ArquivoUser`, tudo que pode
    falhar acontece antes de qualquer escrita: a busca do email no índice e a montagem do
    `ParIDEMAIL`, cujo construtor recusa email que não caiba nos 64 bytes do par. Na troca de email,
    o par novo entra no índice antes de o antigo ser apagado. Assim, uma operação recusada não deixa
    registro sem índice nem índice apontando para registro errado, e a conta continua acessível.
11. **Leitura do teclado na codificação do console.** O `new Scanner(System.in)` decodifica a
    entrada com a codificação padrão da JVM, que no JDK 18 em diante é UTF-8. O console do Windows,
    porém, envia os bytes em cp850, e cada acento digitado chegava ao programa como o caractere de
    substituição, indo assim para o arquivo. O `getTheSameScan()` lê a propriedade `stdin.encoding`
    (com `native.encoding` e UTF-8 como alternativas) e cria o `Scanner` com ela, então nome, texto
    da pergunta, palavras-chave, respostas e resposta secreta preservam a acentuação em qualquer
    terminal.

## Onde e como usamos IA

| Onde | O que a IA fez | O que o grupo fez |
|---|---|---|
| `Security.Crypt.getSHA256` (TP1) | escreveu a implementação do hash SHA-256 em Java, a partir do `MessageDigest` | escolheu SHA-256 no lugar do MD5, por causa da facilidade de força bruta no MD5, e integrou a classe ao cadastro, ao login e à troca de senha |
| `StateView.getTheSameScan` (TP1) | apontou que o `new Scanner(System.in)` decodifica a entrada como UTF-8 enquanto o console do Windows envia cp850, e escreveu a leitura da propriedade `stdin.encoding` | reproduziu o defeito, em que um acento digitado virava `?` dentro do arquivo, aplicou a correção e testou com nome, pergunta e resposta secreta acentuados |
| Revisão e depuração (TP1) | leu o código procurando defeitos, reproduziu cada um em execução e sugeriu as correções pontuais | decidiu o que aceitar, aplicou as alterações e repetiu os testes |
| Roteiro do TP2 | leu o enunciado e o código que o grupo já tinha (as entidades `Resposta` e `Avaliacao` e os arquivos com as duas árvores cada), apontou o que faltava e os defeitos, propôs a árvore por data com o `ParDataId` e o `read(null)`, a navegação com `Supplier`; depois, executou o roteiro de testes no código do grupo, com entrada simulada | aplicou o roteiro passo a passo, decidiu reaproveitar as funções de contagem que já existiam no `ArquivoResposta` em vez de trocá-las por uma nova, escreveu e revisou o código e repetiu os testes |
| Ordenação do resultado da busca (TP3) | sugeriu ordenar os IDs pelo valor TF×IDF com `ids.sort(...)`, trecho marcado no código com o comentário "IA UTILIZADA NESTE TRECHO" | integrou a `ListaInvertida`, escreveu o cálculo do TF e do TF×IDF e a tela de busca |
| Revisão e depuração (TP3) | executou o programa com entrada simulada e apontou os defeitos listados abaixo; escreveu as correções e o teste de estresse da `ArvoreBMais` | revisou as correções e repetiu os testes descritos no checklist |
| Este relatório | atualizou o texto a partir do código e copiou as capturas de uma execução real do programa | revisou o conteúdo e conferiu cada afirmação contra o programa |

Os defeitos encontrados na revisão do TP1 foram a resposta secreta que não era normalizada antes do
hash, o login inválido sem mensagem de erro, o cadastro que voltava ao menu quando o email já
existia e a ordem de escrita no índice de email, que podia deixar uma conta inacessível.

Na revisão do TP2, os defeitos encontrados no código anterior ao roteiro foram: a tela de responder
não chamava `changeState` e encerrava o programa; a avaliação era gravada em três chamadas
separadas, e um erro na primeira não impedia as outras; o arquivamento da resposta sobrescrevia a
data de criação; a exclusão da conta deixava respostas e avaliações órfãs; Minhas respostas sempre
voltava para Minha Área por causa de dois `changeState` seguidos; e o `contains` do caminho de
navegação dava falso positivo com os nomes das telas novas.

Na revisão do TP3, os defeitos encontrados no código da busca foram: o `update` tirava do índice as
palavras-chave novas em vez das antigas, então a busca por uma palavra removida continuava
encontrando a pergunta; a remoção não normalizava os termos, então uma palavra-chave com acento ou
maiúscula nunca saía das listas; as palavras-chave eram indexadas inteiras ("banco de dados") e a
consulta separada por espaço, então uma palavra-chave com mais de uma palavra nunca era encontrada,
e não havia stop words; na tela de busca, o décimo item de cada página não abria, o rodapé mostrava
"Página 1 de 10" para dez resultados e apertar B na última página descartava o resultado; as
perguntas gravadas antes do TP3 não entravam no índice. O
teste de arquivamento revelou ainda o defeito da `ArvoreBMais` descrito no item 8 de
[Do TP3](#do-tp3).

A modelagem das entidades, a estrutura de arquivos, o relacionamento 1:N do TP1, o índice indireto
por email, as entidades `Resposta` e `Avaliacao` com as respectivas árvores e a divisão do código em
MVC com o padrão State foram feitos pelo grupo. No TP3, a integração da `ListaInvertida` ao
`ArquivoPergunta`, o cálculo do TF e do TF×IDF e a tela `StateByKeyWordQuestions` foram feitos pelo
grupo. Os trechos sugeridos por IA foram lidos e integrados pelo grupo e testados com o roteiro
descrito no checklist.

## Checklist

**1. O índice invertido com palavras-chave foi criado usando a classe ListaInvertida?**

**Sim.** O `ArquivoPergunta` usa a `ListaInvertida` do professor, com o par `ElementoLista`
(idPergunta; TF), gravada em `dados/perguntas/DictionaryQuestions.db` e `InvertedQuestions.db`.
Cada pergunta incluída chama `incrementaEntidades()` e grava os termos das palavras-chave com
`create`, ou com `read` e `update` quando o termo se repete. Com as quatro perguntas das capturas,
as listas gravadas ficaram iguais às do exemplo do enunciado, inclusive o par (3; 0,4) de
"inteligencia". A alteração das palavras-chave e a exclusão de uma conta também atualizam o índice,
como mostra a seção [Busca por palavras-chave (TP3)](#busca-por-palavras-chave-tp3).

**2. É possível buscar perguntas por palavras-chave no menu de buscar perguntas?**

**Sim.** Em Buscar perguntas, a opção "[1] Busca por palavras-chave" pede os termos, separados por
espaço. Acentos, maiúsculas e stop words da consulta são ignorados. O resultado aparece de 10 em 10,
com A e B para trocar de página, e cada pergunta abre pelo número, com as respostas. Testado com
uma e com duas palavras, com acento ("Índice", "Inteligência Artificial", "injeção dependências"),
com stop word no meio ("banco de dados"), com 12 resultados (as duas páginas e a abertura do item
12) e com uma busca sem resultado.

**3. As perguntas retornadas estão corretamente classificadas pelo modelo TFxIDF?**

**Sim.** O valor de cada pergunta é a soma de TF × log10(N / tamanho da lista) para cada termo da
consulta, e o resultado sai em ordem decrescente desse valor. Com os dados do exemplo do enunciado,
a busca "Inteligência Artificial" devolveu as perguntas na ordem 1, 3, 2, com valores 0,142, 0,110
e 0,042, e a pergunta 4 ficou de fora, como no enunciado. Com as 100 perguntas do arquivo de teste
`src/perguntas_100.in`, a busca "desenvolvimento web" trouxe 7 perguntas: primeiro as quatro que têm
os dois termos (0,631 cada), depois as que têm só "web" com 3 e com 4 termos (0,407 e 0,305) e por
último a que tem só "desenvolvimento" com 5 termos (0,260). Os valores foram conferidos à mão.
Perguntas com o mesmo valor não têm ordem definida entre si.

**4. O trabalho compila corretamente?**

**Sim.** Compila sem erros com o JDK 25.0.1, pelos comandos da seção "Como compilar e executar".

**5. O trabalho está completo e funcionando sem erros de execução?**

**Sim.** As duas tarefas do TP3 estão implementadas: o índice invertido das palavras-chave e a busca
ordenada por TF×IDF. O roteiro de teste, com entrada simulada, passou por: inclusão de 16 perguntas
e, numa segunda base, das 100 perguntas de `src/perguntas_100.in`; buscas com uma e com duas
palavras, com acento e com stop word; busca antes e depois da alteração das palavras-chave, em que a
palavra removida deixou de encontrar a pergunta e as novas passaram a encontrar; arquivamento, em
que a pergunta saiu do resultado; exclusão de uma conta, em que o número de perguntas do índice caiu
de 4 para 2 e os IDs saíram de todas as listas; reconstrução do índice a partir de dados já
gravados; e paginação com 12 resultados. Nenhum caminho lançou exceção. Os defeitos encontrados
durante esses testes, inclusive o da `ArvoreBMais`, foram corrigidos antes da entrega e estão
listados em [Onde e como usamos IA](#onde-e-como-usamos-ia).

**6. O trabalho é original e não a cópia de um trabalho de outro grupo?**

**Sim.** O trabalho foi desenvolvido pelo grupo, sobre o código-base fornecido pelo professor. O uso
de IA está descrito na seção [Onde e como usamos IA](#onde-e-como-usamos-ia).
