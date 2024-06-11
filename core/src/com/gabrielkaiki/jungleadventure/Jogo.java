package com.gabrielkaiki.jungleadventure;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Jogo extends ApplicationAdapter implements ApplicationListener {

    //Texturas
    private SpriteBatch batch;
    private Texture[] monkey;
    private HashMap<String, Texture[]> inimigos;
    private Texture[] skunk;
    private Texture[] monster;
    private Texture[] slug;
    private Texture[] bee;
    private Texture fundo;
    private Texture gameOver;
    private Texture nomeJogo;
    private Texture toqueParaIniciar;
    private Texture restart;

    //Formas para colisão
    private ShapeRenderer shapeRenderer;
    private Rectangle retanguloMacaco;
    private Rectangle retanguloInimigo;

    //Atributos de configurações
    private float larguraDispositivo;
    private float alturaDispositivo;
    private float variacao = 0;
    private int indiceInimigos = 0;
    private List<String> nomesDosInimigos;
    private float gravidade = 2;
    private float posicaoInicialVerticalMacaco = 0;
    private float posicaoInimigoHorizontal;
    private float posicaoInimigoVertical;
    private Random random;
    private int pontos = 0;
    private float variacaoTexturasInimigos = 0;
    private int pontuacaoMaxima = 0;
    private boolean passouCano = false;
    private boolean puloCompleto = false;
    private boolean tocandoSom = false;
    private boolean controleRepeticaoRender = false;
    private int estadoJogo = 0;
    private float posicaoHorizontalMacaco = 0;
    private int contadorPulo = 0;
    private int valorLarguraTela = 0;
    private int valorAlturaTela = 0;

    //Exibiçao de textos
    BitmapFont textoPontuacao;
    BitmapFont textoReiniciar;
    BitmapFont textoMelhorPontuacao;

    //Configuração dos sons
    Sound somColisao;
    Sound somPontuacao;
    Sound somSlug;
    Sound somBee;
    Sound somInimigo;
    Sound somMacaco;
    Sound somGosma;
    Sound somPatas;

    //Músicas
    Music selva;

    //Objeto salvar pontuacao
    Preferences preferencias;

    //Objetos para câmera
    private OrthographicCamera camera;
    private Viewport viewport;
    private final float VIRTUAL_WIDTH = 2000;
    private final float VIRTUAL_HEIGHT = 850;
    private AdController adController;

    public Jogo(AdController adC) {
        adController = adC;
    }

    @Override
    public void create() {
        inicializarTexturas();
        inicializaObjetos();
    }

    @Override
    public void render() {

        // Limpar frames anteriores
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        verificarEstadoJogo();
        validarPontos();
        detectarColisoes();
        desenharTexturas();

    }

    /*
     * 0 - Jogo inicial, passaro parado
     * 1 - Começa o jogo
     * 2 - Colidiu
     *
     */
    private void verificarEstadoJogo() {

        boolean toqueTela = Gdx.input.justTouched();

        if (estadoJogo == 0) {
            variacao = 0;
            /* Aplica evento de toque na tela */
            if (toqueTela) {
                somMacaco.play();
                estadoJogo = 1;
            }

        } else if (estadoJogo == 1) {
            selva.setLooping(true);
            selva.play();

            /* Aplica evento de toque na tela */
            if (toqueTela && contadorPulo < 2) {
                somMacaco.play();
                gravidade = -25;
                contadorPulo++;
            } else {
                if (posicaoInicialVerticalMacaco <= 0) contadorPulo = 0;
            }

            //somSelva.play();

            /*Movimentar o obstáculo*/
            Texture[] spritersInimigo = inimigos.get(nomesDosInimigos.get(indiceInimigos));

            if (pontos < 10) {
                posicaoInimigoHorizontal -= Gdx.graphics.getDeltaTime() * 880;
            } else if (pontos < 20) {
                posicaoInimigoHorizontal -= Gdx.graphics.getDeltaTime() * 1200;
            } else if (pontos < 30) {
                posicaoInimigoHorizontal -= Gdx.graphics.getDeltaTime() * 1500;
            } else {
                posicaoInimigoHorizontal -= Gdx.graphics.getDeltaTime() * 1700;
            }

            if (posicaoInimigoHorizontal < -spritersInimigo[(int) indiceInimigos].getWidth()) {
                posicaoInimigoHorizontal = larguraDispositivo;
                posicaoInimigoVertical = 0;
                indiceInimigos = random.nextInt(4);
                passouCano = false;
                controleRepeticaoRender = false;
                puloCompleto = false;
                pontos++;
                somPontuacao.play();
                if (tocandoSom) somInimigo.stop();
                tocandoSom = false;
            }

            /* Aplica gravidade no pássaro */
            if (posicaoInicialVerticalMacaco > 0 || toqueTela) {
                posicaoInicialVerticalMacaco = posicaoInicialVerticalMacaco - gravidade;
            } else {
                posicaoInicialVerticalMacaco = 0;
            }

            gravidade++;
        } else if (estadoJogo == 2) {
            selva.pause();

            if (pontos > pontuacaoMaxima) {
                pontuacaoMaxima = pontos;
                preferencias.putInteger("pontuacaoMaxima", pontuacaoMaxima);
                preferencias.flush();
            }

            /* Aplica evento de toque na tela */
            if (toqueTela) {
                adController.showIntersticialAd();
                estadoJogo = 0;
                pontos = 0;
                gravidade = 0;
                posicaoHorizontalMacaco = 0;
                posicaoInicialVerticalMacaco = 0;
                posicaoInimigoVertical = 0;
                posicaoInimigoHorizontal = larguraDispositivo;
            }

        }

    }

    private void exibirAnuncio() {

    }

    private void detectarColisoes() {

        Texture[] spritersInimigo = inimigos.get(nomesDosInimigos.get(indiceInimigos));

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        /*shapeRenderer.rect(100 + posicaoPeixeX, posicaoPeixeY, player[(int) variacao].getWidth(), player[(int) variacao].getHeight());
        shapeRenderer.rect(posicaoObjFundoX, posicaoObjFundoY, objetoFundo.getWidth(), objetoFundo.getHeight());*/

        shapeRenderer.circle(120, 550, 80);
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.end();

        retanguloMacaco.set(100 + posicaoHorizontalMacaco, posicaoInicialVerticalMacaco, monkey[(int) variacao].getWidth(), monkey[(int) variacao].getHeight());

        retanguloInimigo.set(posicaoInimigoHorizontal, posicaoInimigoVertical, spritersInimigo[(int) variacaoTexturasInimigos].getWidth(), spritersInimigo[(int) variacaoTexturasInimigos].getHeight());

        boolean colidiuInimigo = Intersector.overlaps(retanguloMacaco, retanguloInimigo);

        if (colidiuInimigo) {
            if (estadoJogo == 1) {
                somColisao.play();
                somInimigo.stop();
                estadoJogo = 2;
            }
        }
    }

    private void desenharTexturas() {
        batch.setProjectionMatrix(camera.combined);
        Texture[] spritersInimigo = inimigos.get(nomesDosInimigos.get(indiceInimigos));

        batch.begin();
        //Fundo
        batch.draw(fundo, 0, 0, larguraDispositivo, alturaDispositivo);

        //Nome do jogo na tela inicial
        if (estadoJogo == 0) {
            batch.draw(nomeJogo, VIRTUAL_WIDTH / 2 - nomeJogo.getWidth() / 2, VIRTUAL_HEIGHT / 2);
            batch.draw(toqueParaIniciar, VIRTUAL_WIDTH / 2 - toqueParaIniciar.getWidth() / 2, VIRTUAL_HEIGHT / 2 - nomeJogo.getHeight() - 50);
        }

        //Texturas desenhadas durante o jogo
        if (estadoJogo != 2) {
            batch.draw(monkey[(int) variacao], 100 + posicaoHorizontalMacaco, posicaoInicialVerticalMacaco);
        } else {
            batch.draw(monkey[monkey.length - 1], 100, posicaoInicialVerticalMacaco);
        }
        batch.draw(spritersInimigo[(int) variacaoTexturasInimigos], posicaoInimigoHorizontal, posicaoInimigoVertical);
        textoPontuacao.draw(batch, "score: " + pontos, larguraDispositivo - 350, alturaDispositivo - 30);

        //Texturas desenhadas na tela de game over
        if (estadoJogo == 2) {
            batch.draw(gameOver, VIRTUAL_WIDTH / 2 - gameOver.getWidth() / 2, VIRTUAL_HEIGHT / 2);
            batch.draw(restart, VIRTUAL_WIDTH / 2 - restart.getWidth() / 2, VIRTUAL_HEIGHT / 2 - gameOver.getHeight() / 2 - 30);
            textoMelhorPontuacao.draw(batch, "Maximum score: " + pontuacaoMaxima + " points", VIRTUAL_WIDTH / 2 - (VIRTUAL_WIDTH/6), VIRTUAL_HEIGHT / 2 - gameOver.getHeight() - 50);
        }
        batch.end();
    }

    public void validarPontos() {

        if (indiceInimigos > 3) {
            indiceInimigos = 0;
        }
        verificaInimigo(indiceInimigos);

        variacao += Gdx.graphics.getDeltaTime() * 10;
        /* Verifica variação para bater asas do pássaro*/

        if (variacao > 7) variacao = 0;
    }

    private void verificaInimigo(int identificadorInimigos) {
        switch (identificadorInimigos) {
            case 0:
            case 1:
                variacaoTexturasInimigos += Gdx.graphics.getDeltaTime() * 10;

                if (variacaoTexturasInimigos > 3) variacaoTexturasInimigos = 0;

                if (identificadorInimigos == 0) {
                    if (!tocandoSom) {
                        somInimigo = somPatas;
                        somInimigo.loop();
                        tocandoSom = true;
                    }
                } else if (identificadorInimigos == 1) {
                    if (!tocandoSom) {
                        somInimigo = somGosma;
                        somInimigo.loop();
                        tocandoSom = true;
                    }
                }


                if (pontos > 10) {
                    adicionarFuncaoDePulo();
                }
                break;

            case 2:
                variacaoTexturasInimigos += Gdx.graphics.getDeltaTime() * 10;

                if (variacaoTexturasInimigos > 2) variacaoTexturasInimigos = 0;

                if (!tocandoSom) {
                    somInimigo = somSlug;
                    somInimigo.loop();
                    tocandoSom = true;
                }
                break;

            case 3:
                variacaoTexturasInimigos += Gdx.graphics.getDeltaTime() * 10;

                if (variacaoTexturasInimigos > 4) variacaoTexturasInimigos = 0;

                variacaoVerticalAbelha();

                if (!tocandoSom) {
                    somInimigo = somBee;
                    somInimigo.loop();
                    tocandoSom = true;
                }
                break;
        }
    }

    private boolean variacaoAbelha = false;

    private void variacaoVerticalAbelha() {
        if (pontos > 10) {
            if (!variacaoAbelha) {
                posicaoInimigoVertical += 20;
                if (posicaoInimigoVertical > 250) variacaoAbelha = true;
            } else {
                posicaoInimigoVertical -= 20;
                if (posicaoInimigoVertical < 0) {
                    posicaoInimigoVertical = 0;
                    variacaoAbelha = false;
                }
            }
        }
    }

    private int localPulo = 0;

    private void adicionarFuncaoDePulo() {
        if (!controleRepeticaoRender) {
            localPulo = random.nextInt((int) VIRTUAL_WIDTH);
            controleRepeticaoRender = true;
        }

        if (!puloCompleto) {
            if (posicaoInimigoHorizontal < localPulo) {
                posicaoInimigoVertical += 20;
                posicaoInimigoHorizontal -= 20;
                if (posicaoInimigoVertical > 200) puloCompleto = true;
            }
        } else {
            posicaoInimigoVertical -= 20;
            if (posicaoInimigoVertical < 0) posicaoInimigoVertical = 0;
        }
    }

    private void inicializarTexturas() {

        fundo = new Texture("jungle.jpg");
        gameOver = new Texture("game_over.png");
        nomeJogo = new Texture("texto_nome_jogo.png");
        toqueParaIniciar = new Texture("texto_tap_screen_start.png");
        restart = new Texture("tap_to_restart.png");

    }

    private void inicializaObjetos() {

        batch = new SpriteBatch();
        random = new Random();

        monkey = new Texture[9];
        monkey[0] = new Texture("monkey_run_1.png");
        monkey[1] = new Texture("monkey_run_2.png");
        monkey[2] = new Texture("monkey_run_3.png");
        monkey[3] = new Texture("monkey_run_4.png");
        monkey[4] = new Texture("monkey_run_5.png");
        monkey[5] = new Texture("monkey_run_6.png");
        monkey[6] = new Texture("monkey_run_7.png");
        monkey[7] = new Texture("monkey_run_8.png");
        monkey[8] = new Texture("monkey_dead.png");

        skunk = new Texture[4];
        skunk[0] = new Texture("skunk_walk_01.png");
        skunk[1] = new Texture("skunk_walk_02.png");
        skunk[2] = new Texture("skunk_walk_03.png");
        skunk[3] = new Texture("skunk_walk_04.png");

        monster = new Texture[4];
        monster[0] = new Texture("Monster4_01.png");
        monster[1] = new Texture("Monster4_02.png");
        monster[2] = new Texture("Monster4_03.png");
        monster[3] = new Texture("Monster4_04.png");

        slug = new Texture[4];
        slug[0] = new Texture("slug_1.png");
        slug[1] = new Texture("slug_2.png");
        slug[2] = new Texture("slug_3.png");

        bee = new Texture[5];
        bee[0] = new Texture("bee_1.png");
        bee[1] = new Texture("bee_2.png");
        bee[2] = new Texture("bee_3.png");
        bee[3] = new Texture("bee_4.png");
        bee[4] = new Texture("bee_5.png");

        nomesDosInimigos = new ArrayList(2);
        nomesDosInimigos.add("skunk");
        nomesDosInimigos.add("monster");
        nomesDosInimigos.add("slug");
        nomesDosInimigos.add("bee");

        inimigos = new HashMap<>(2);
        inimigos.put("skunk", skunk);
        inimigos.put("monster", monster);
        inimigos.put("slug", slug);
        inimigos.put("bee", bee);

        larguraDispositivo = VIRTUAL_WIDTH;
        alturaDispositivo = VIRTUAL_HEIGHT;
        posicaoInicialVerticalMacaco = 0;
        posicaoInimigoHorizontal = larguraDispositivo;
        posicaoInimigoVertical = 0;
        valorLarguraTela = Gdx.graphics.getWidth();
        valorAlturaTela = Gdx.graphics.getHeight();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font/sixty.TTF"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 70;
        parameter.borderColor = Color.BLACK;
        parameter.color = Color.RED;
        parameter.borderWidth = 3;

        //Configurações dos textos
        textoPontuacao = generator.generateFont(parameter);

        textoReiniciar = generator.generateFont(parameter);

        textoMelhorPontuacao = generator.generateFont(parameter);

        generator.dispose();

        //Formas Geeométricas para colisoes;
        shapeRenderer = new ShapeRenderer();
        retanguloMacaco = new Rectangle();
        retanguloInimigo = new Rectangle();

        //Inicializa sons
        somColisao = Gdx.audio.newSound(Gdx.files.internal("som_batida.wav"));
        somPontuacao = Gdx.audio.newSound(Gdx.files.internal("som_pontos.wav"));
        somSlug = Gdx.audio.newSound(Gdx.files.internal("slug_sound.mp3"));
        somBee = Gdx.audio.newSound(Gdx.files.internal("bee_sound.mp3"));
        somMacaco = Gdx.audio.newSound(Gdx.files.internal("monkey_som.mp3"));
        somGosma = Gdx.audio.newSound(Gdx.files.internal("gosma_som.mp3"));
        somPatas = Gdx.audio.newSound(Gdx.files.internal("patas_som.mp3"));

        //Músicas
        selva = Gdx.audio.newMusic(Gdx.files.internal("som_selva.mp3"));

        //Configura preferências dos objetos
        preferencias = Gdx.app.getPreferences("flappyBird");
        pontuacaoMaxima = preferencias.getInteger("pontuacaoMaxima", 0);

        //Configuração da câmera
        camera = new OrthographicCamera();
        camera.position.set(VIRTUAL_WIDTH / 2, VIRTUAL_HEIGHT / 2, 0);
        viewport = new StretchViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);

    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void dispose() {

    }
}
