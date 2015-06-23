package br.furb.bte;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.media.opengl.DebugGL;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.glu.GLU;
import javax.swing.JOptionPane;
import br.furb.bte.objetos.Arena;
import br.furb.bte.objetos.Cor;
import br.furb.bte.objetos.Moto;
import br.furb.bte.objetos.Mundo;
import com.sun.opengl.util.GLUT;
import com.sun.opengl.util.j2d.TextRenderer;
import com.sun.opengl.util.texture.TextureData;

public class Tela extends GLCanvas implements GLEventAdapter {

    private class RenderLoop extends Thread {

	public RenderLoop() {
	    super("RenderLoop");
	}

	@Override
	public void run() {
	    while (true) {
		try {
		    if (animando || jogando || executandoPasso) {
			//			System.out.println("Tela.RenderLoop.run()");
			if (jogando) {
			    fireGameplayEvent(l -> l.beforePlay());
			    executarComportamentos();
			}
			glDrawable.display();
			Thread.sleep(30);
		    } else {
			synchronized (this) {
			    wait();
			}
		    }
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
	    }
	}
    }

    private final Collection<GameplayListener> listeners;

    private static final long serialVersionUID = 1L;
    private static final int NEAR = 1;
    private static final int FAR = 2000;
    private static final TextRenderer TEXT_RENDERER = new TextRenderer(new Font("SansSerif", Font.BOLD, 18), true,
	    false);
    private static final int TAMANHO_ARENA = 500;

    // ========== RECURSOS DO CANVAS ==========
    private int largura = 800;
    private int altura = 600;
    private GL gl;
    private GLU glu;
    private GLAutoDrawable glDrawable;
    private boolean atualizarVisualizacao;
    private boolean perspectiveMode = true;
    private final float[] posicaoLuz = { 25, 100, 25, 0.1f };
    private final float[] direcaoLuz = { 0, 0, 0 };
    private final float[] luzAmbiente = { 0.2f, 0.2f, 0.2f, 0.2f };
    // ========== OBJETOS GRÃ�FICOS ==========
    private Mundo mundo;
    private Moto moto1;
    private Moto moto2;
    private Optional<Moto> motoJogador = Optional.empty();
    private Arena arena;
    private Camera camera;

    // ========== CONTROLES DE EXECUÃ‡ÃƒO ==========
    private EstadoJogo estadoJogo;
    private boolean jogando;
    private boolean animando = true;
    private boolean executandoPasso;

    private final RenderLoop renderLoop;

    private int idTexture[];
    private int width, height;
    private BufferedImage image;
    private TextureData td;
    private ByteBuffer buffer;

    public Tela(GLCapabilities capabilities) {
	super(capabilities);
	setPreferredSize(new Dimension(largura, altura));
	renderLoop = new RenderLoop();
	listeners = new ArrayList<>();
	addGLEventListener(this);
    }

    public void addGameplayListener(GameplayListener l) {
	listeners.add(l);
    }

    public void removeGameplayListener(GameplayListener l) {
	listeners.remove(l);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
	glDrawable = drawable;
	gl = drawable.getGL();
	gl.glEnable(GL.GL_DEPTH_TEST);
	gl.glEnable(GL.GL_CULL_FACE);

	gl.glEnable(GL.GL_POINT_SMOOTH);
	gl.glEnable(GL.GL_POLYGON_SMOOTH);
	gl.glEnable(GL.GL_LINE_SMOOTH);

	gl.glHint(GL.GL_POINT_SMOOTH_HINT, GL.GL_NICEST);
	gl.glHint(GL.GL_POLYGON_SMOOTH_HINT, GL.GL_NICEST);
	gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);

	glu = new GLU();
	glDrawable.setGL(new DebugGL(gl));
	gl.glClearColor(0f, 0f, 0f, 1.0f);

	mundo = new Mundo();

	camera = new Camera(this);

	// Habilita o modelo de colorização de Gouraud
	gl.glShadeModel(GL.GL_SMOOTH);

	// Comandos de inicialização para textura
	// loadImage("madeira.jpg");
	loadImage("data/teste.jpg");

	// Gera identificador de textura
	idTexture = new int[10];
	gl.glGenTextures(1, idTexture, 1);

	// Especifica qual é a textura corrente pelo identificador
	gl.glBindTexture(GL.GL_TEXTURE_2D, idTexture[0]);

	// Envio da textura para OpenGL
	gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, 3, width, height, 0, GL.GL_BGR, GL.GL_UNSIGNED_BYTE, buffer);

	// Define os filtros de magnificação e minificação
	gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
	gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);

	addComponentListener(new ComponentAdapter() {

	    @Override
	    public void componentResized(ComponentEvent e) {
		if (arena != null) {
		    render();
		}
	    }
	});
	fireGameplayEvent(l -> l.afterInit());

	reset();
	renderLoop.start();
    }

    public void loadImage(String fileName) {
	// Tenta carregar o arquivo
	image = null;
	try {
	    image = ImageIO.read(new File(fileName));
	} catch (IOException e) {
	    JOptionPane.showMessageDialog(null, "Erro na leitura do arquivo " + fileName);
	}

	// Obtém largura e altura
	width = image.getWidth();
	height = image.getHeight();
	// Gera uma nova TextureData...
	td = new TextureData(0, 0, false, image);
	// ...e obtém um ByteBuffer a partir dela
	buffer = (ByteBuffer) td.getBuffer();
    }

    private void fireGameplayEvent(Consumer<GameplayListener> consumer) {
	for (GameplayListener l : listeners) {
	    consumer.accept(l);
	}
    }

    private void configurarCanvas() {
	gl.glMatrixMode(GL.GL_PROJECTION);
	gl.glLoadIdentity();
	if (perspectiveMode) {
	    float aspect = largura / (float) altura;
	    glu.gluPerspective(60, aspect, NEAR, FAR);
	} else {
	    gl.glOrtho(-largura, largura, -altura, altura, NEAR, FAR);
	}
	gl.glMatrixMode(GL.GL_MODELVIEW);
	gl.glLoadIdentity();

	gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, posicaoLuz, 0);
	gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPOT_DIRECTION, direcaoLuz, 0);
	//	gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPOT_EXPONENT, new float[] { 0 }, 0);
	gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, luzAmbiente, 0);
	gl.glEnable(GL.GL_LIGHT0);
	gl.glEnable(GL.GL_LIGHTING);
	gl.glShadeModel(GL.GL_SMOOTH);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
	this.altura = height;
	this.largura = width;
	setPreferredSize(new Dimension(largura, altura));
	configurarCanvas();
    }

    @Override
    public void display(GLAutoDrawable arg0) {
	if (atualizarVisualizacao) {
	    atualizarVisualizacao = false;
	    configurarCanvas();
	}
	gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
	gl.glLoadIdentity();
	// transformacaoMundo =
	// camera.absorverTransformacao(transformacaoMundo);
	// gl.glPushMatrix();
	// {
	// gl.glMultMatrixd(transformacaoMundo.getMatriz(), 0);

	animando = camera.atualizar(glu);
	animando |= mundo.renderizar(gl);
	//	drawCube(30, 100, 30);
	desenhaSRU(gl);
	// new GLUT().glutSolidCube(100);
	// gl.glFlush();
	// }
	// gl.glPopMatrix();
	//
	drawWalls();

	TEXT_RENDERER.beginRendering(largura, altura);
	{
	    TEXT_RENDERER.setColor(1, 1, 1, 1);
	    int meioLargura = largura / 2;
	    int meioAltura = altura / 2;
	    if (estadoJogo != null && !jogando) {
		TEXT_RENDERER.draw(estadoJogo.getMensagemFimDeJogo(), meioLargura - 25, meioAltura);
		TEXT_RENDERER.draw("R para reiniciar", meioLargura - 50, meioAltura - 20);
	    } else {
		if (jogando) {
		    TEXT_RENDERER.draw("Rodando", 0, altura - 25);
		} else {
		    TEXT_RENDERER.draw("Pausado", meioLargura - 25, meioAltura);
		    TEXT_RENDERER.draw("Espaço para continuar", meioLargura - 80, meioAltura - 20);
		}
	    }
	}
	TEXT_RENDERER.endRendering();
    }

    private void drawCube(float xS, float yS, float zS) {
	float[] red = { 1f, 0f, 0f, 1f };
	gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT_AND_DIFFUSE, red, 0);

	gl.glPushMatrix();
	{
	    gl.glScalef(xS, yS, zS);
	    new GLUT().glutSolidCube(1.0f);
	}
	gl.glPopMatrix();
    }

    private void drawWalls() {
	gl.glDisable(GL.GL_CULL_FACE);
	{
	    float[] cor = { 0, 1, 0, 1f };
	    gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT_AND_DIFFUSE, cor, 0);
	    // Desenha um cubo no qual a textura é aplicada
	    //	    gl.glEnable(GL.GL_TEXTURE_2D); // Primeiro habilita uso de textura
	    //	    {
	    //		gl.glColor3f(0.0f, 1.0f, 0.0f);
	    gl.glPushMatrix();
	    {
		gl.glBegin(GL.GL_QUADS);
		{
		    gl.glVertex3f(-500, 0f, +500);
		    gl.glVertex3f(-500, 0f, +550f);
		    gl.glVertex3f(+500, 0f, +550f);
		    gl.glVertex3f(+500, 0f, +500);
		    // face de cima
		    gl.glVertex3f(-500, 50f, +500);
		    gl.glVertex3f(-500, 50f, +550f);
		    gl.glVertex3f(+500, 50f, +550f);
		    gl.glVertex3f(+500, 50f, +500);
		    // face da frente
		    gl.glVertex3f(-500, 0f, +500);
		    gl.glVertex3f(-500, 50f, +500);
		    gl.glVertex3f(+500, 50f, +500);
		    gl.glVertex3f(+500, 0f, +500);
		    // face da traz
		    gl.glVertex3f(-500, 0f, +550f);
		    gl.glVertex3f(-500, 50f, +550f);
		    gl.glVertex3f(+500, 50f, +550f);
		    gl.glVertex3f(+500, 0f, +550f);
		    // face lateral esquerda
		    gl.glVertex3f(-500, 0f, +500);
		    gl.glVertex3f(-500, 50f, +500);
		    gl.glVertex3f(-500, 50f, +550f);
		    gl.glVertex3f(-500, 0f, +550f);
		    // face lateral direita
		    gl.glVertex3f(+500, 0f, +500);
		    gl.glVertex3f(+500, 50f, +500);
		    gl.glVertex3f(+500, 50f, +550f);
		    gl.glVertex3f(+500, 0f, +550f);
		}
		gl.glEnd();
		gl.glBegin(GL.GL_QUADS);
		{
		    /**
		     * Segundo MURO
		     */
		    gl.glVertex3f(-500, 0f, -500);
		    gl.glVertex3f(-500, 0f, -550f);
		    gl.glVertex3f(+500, 0f, -550f);
		    gl.glVertex3f(+500, 0f, -500);
		    // face de cima
		    gl.glVertex3f(-500, 50f, -500);
		    gl.glVertex3f(-500, 50f, -550f);
		    gl.glVertex3f(+500, 50f, -550f);
		    gl.glVertex3f(+500, 50f, -500);
		    // face da frente
		    gl.glVertex3f(-500, 0f, -500);
		    gl.glVertex3f(-500, 50f, -500);
		    gl.glVertex3f(+500, 50f, -500);
		    gl.glVertex3f(+500, 0f, -500);
		    // face da traz
		    gl.glVertex3f(-500, 0f, -550f);
		    gl.glVertex3f(-500, 50f, -550f);
		    gl.glVertex3f(+500, 50f, -550f);
		    gl.glVertex3f(+500, 0f, -550f);
		    // face lateral esquerda
		    gl.glVertex3f(-500, 0f, -500);
		    gl.glVertex3f(-500, 50f, -500);
		    gl.glVertex3f(-500, 50f, -550f);
		    gl.glVertex3f(-500, 0f, -550f);
		    // face lateral direita
		    gl.glVertex3f(+500, 0f, -500);
		    gl.glVertex3f(+500, 50f, -500);
		    gl.glVertex3f(+500, 50f, -550f);
		    gl.glVertex3f(+500, 0f, -550f);
		}
		gl.glEnd();
		gl.glBegin(GL.GL_QUADS);
		{
		    /**
		     * Terceiro Muro
		     */
		    gl.glVertex3f(-500, 0f, +500);
		    gl.glVertex3f(-550f, 0f, -500);
		    gl.glVertex3f(-550f, 0f, +500);
		    gl.glVertex3f(-500, 0f, -500);
		    // face de cima
		    gl.glVertex3f(-500, 50f, -500);
		    gl.glVertex3f(-500, 50f, -550f);
		    gl.glVertex3f(+500, 50f, -550f);
		    gl.glVertex3f(+500, 50f, -500);
		    // face da frente
		    gl.glVertex3f(-500, 0f, +500);
		    gl.glVertex3f(-500, 50f, +500);
		    gl.glVertex3f(-500, 50f, -500);
		    gl.glVertex3f(-500, 0f, -500);
		    // face da traz
		    gl.glVertex3f(-550f, 0f, +500);
		    gl.glVertex3f(-550f, 50f, +500);
		    gl.glVertex3f(-550f, 50f, -500);
		    gl.glVertex3f(-550f, 0f, -500);
		    // face lateral esquerda
		    gl.glVertex3f(-500, 0f, +500);
		    gl.glVertex3f(-500, 50f, +500);
		    gl.glVertex3f(-550f, 50f, -500);
		    gl.glVertex3f(-550f, 0f, -500);
		    // face lateral direita
		    gl.glVertex3f(-500, 0f, -500);
		    gl.glVertex3f(-500, 50f, -500);
		    gl.glVertex3f(-550f, 50f, -500);
		    gl.glVertex3f(-550f, 0f, -500);
		}
		gl.glEnd();
		gl.glBegin(GL.GL_QUADS);
		{
		    /**
		     * Quarto Muro
		     */
		    gl.glVertex3f(+500, 0f, +500);
		    gl.glVertex3f(+550f, 0f, +500);
		    gl.glVertex3f(+550f, 0f, -500);
		    gl.glVertex3f(+500, 0f, -500);
		    // face de cima
		    gl.glVertex3f(+500, 50f, +500);
		    gl.glVertex3f(+550f, 50f, +500);
		    gl.glVertex3f(+550f, 50f, -500);
		    gl.glVertex3f(+500, 50f, -500);
		    // face da frente
		    gl.glVertex3f(+500, 0f, +500);
		    gl.glVertex3f(+500, 50f, +500);
		    gl.glVertex3f(+500, 50f, -500);
		    gl.glVertex3f(+500, 0f, -500);
		    // face da traz
		    gl.glVertex3f(+550f, 0f, +500);
		    gl.glVertex3f(+550f, 50f, +500);
		    gl.glVertex3f(+550f, 50f, -500);
		    gl.glVertex3f(+550f, 0f, -500);
		    // face lateral esquerda
		    gl.glVertex3f(+500, 0f, -500);
		    gl.glVertex3f(+500, 50f, -500);
		    gl.glVertex3f(+550f, 50f, -500);
		    gl.glVertex3f(+550f, 0f, -500);
		    // face lateral direita
		    gl.glVertex3f(+500, 0f, +500);
		    gl.glVertex3f(+500, 50f, +500);
		    gl.glVertex3f(+550f, 50f, +500);
		    gl.glVertex3f(+550f, 0f, +500);

		}
		gl.glEnd();
	    }
	    gl.glPopMatrix();
	    //	    }
	    //	    gl.glDisable(GL.GL_TEXTURE_2D); //	Desabilita uso de textura
	    // gl.glColor3f(0.0f, 1.0f, 0.0f);
	    // gl.glPushMatrix();
	    // gl.glBegin(GL.GL_QUADS );
	    // gl.glEnd();
	    // gl.glPopMatrix();
	}
	gl.glEnable(GL.GL_CULL_FACE);
    }

    private void alterarEstadoJogo(EstadoJogo novoEstado) {
	this.estadoJogo = novoEstado;
	render();
    }

    public void alterarExecucao(boolean executar) {
	this.jogando = executar;
	camera.seguirMoto(executar ? motoJogador.orElse(moto1) : null);

	if (executar) {
	    fireGameplayEvent(l -> l.onResume());
	} else {
	    fireGameplayEvent(l -> l.onPause());
	}
	render();
    }

    public boolean isExecutando() {
	return jogando;
    }

    public void reset() {
	animando = false;

	mundo.removerTodosFilhos();
	arena = new Arena(TAMANHO_ARENA, TAMANHO_ARENA);
	moto1 = new Moto("Player 1", arena.getBBox().getMenorX() + 50, 0, new Cor(1, 0, 0), gl);
	camera.seguirMoto(moto1);
	arena.addMoto(moto1);

	moto2 = new Moto("Player 2", arena.getBBox().getMaiorX() - 50, 0, new Cor(0, 1, 0), gl);
	moto2.girar(180);
	arena.addMoto(moto2);

	mundo.addFilho(arena);
	jogando = false;
	estadoJogo = null;

	animando = true;

	fireGameplayEvent(l -> l.onReset());

	render();
    }

    public void setMotoJogador(Moto moto) {
	if (moto == null || moto != moto1 && moto != moto2) {
	    throw new IllegalArgumentException("a moto informada não faz parte do jogo");
	}
	this.motoJogador = Optional.ofNullable(moto);
    }

    public void executarComportamentos() {
	moto1.mover();
	moto2.mover();

	boolean teveColisao1 = false;
	boolean teveColisao2 = false;

	Moto motoJogadorReal = motoJogador.orElse(moto1);
	Moto inimigo = getInimigo(motoJogadorReal);

	teveColisao1 = verificaColisaoMoto(motoJogadorReal);
	teveColisao2 = verificaColisaoMoto(inimigo);
	if (teveColisao1 || teveColisao2) {
	    alterarExecucao(false);
	    if (teveColisao1 && teveColisao2) {
		alterarEstadoJogo(EstadoJogo.EMPATADO);
	    } else if (teveColisao1) {
		alterarEstadoJogo(EstadoJogo.DERROTADO);
	    } else {
		alterarEstadoJogo(EstadoJogo.VITORIOSO);
	    }
	}
    }

    /**
     * Alterna o modo de visualização entre perspectiva e ortogonal.
     * 
     * @return {@code true} se passou para o modo de perspectiva; {@code false} se passou para o
     *         modo ortogonal
     */
    public boolean alternarPerspectiva() {
	perspectiveMode = !perspectiveMode;
	atualizarVisualizacao = true;
	return perspectiveMode;
    }

    private boolean verificaColisaoMoto(Moto moto) {
	boolean colidido = moto.estaColidindo(moto1.getRastro()) //
		|| moto.estaColidindo(moto2.getRastro()) //
		|| moto.estaColidindo(getInimigo(moto)) //
		|| !moto.estaSobre(arena);

	moto.setColidido(colidido);
	return colidido;
    }

    private Moto getInimigo(Moto moto) {
	return moto == this.moto1 ? moto2 : moto1;
    }

    public void render() {
	//	System.out.println("Tela.render()");
	// forÃ§a que sÃ³ execute quando o RenderLoop chegar no wait(), garantindo
	// que
	// o comportamento vai terminar de executar antes de renderizar
	// novamente
	synchronized (renderLoop) {
	    glDrawable.display();
	    renderLoop.notify();
	}
    }

    public Camera getCamera() {
	return camera;
    }

    public Arena getArena() {
	return arena;
    }

    public boolean isGameOver() {
	return estadoJogo != null;
    }

    /**
     * Desenha os eixos do Sistema de ReferÃªncia Universal
     */
    public static void desenhaSRU(GL gl) {
	//	gl.glEnable(GL.GL_COLOR_MATERIAL);

	gl.glDisable(GL.GL_LIGHTING);

	gl.glLineWidth(1.0f);

	// eixo x
	gl.glColor3f(1.0f, 0.0f, 0.0f);
	gl.glBegin(GL.GL_LINES);
	{
	    gl.glVertex3f(0, 0f, 0f);
	    gl.glVertex3f(200f, 0f, 0f);
	}
	gl.glEnd();

	// eixo y
	gl.glColor3f(0.0f, 1.0f, 0.0f);
	gl.glBegin(GL.GL_LINES);
	{
	    gl.glVertex3f(0f, 0f, 0f);
	    gl.glVertex3f(0f, 200f, 0f);
	}
	gl.glEnd();

	// eixo z
	gl.glColor3f(0.0f, 0.0f, 1.0f);
	gl.glBegin(GL.GL_LINES);
	{
	    gl.glVertex3f(0f, 0f, 0f);
	    gl.glVertex3f(0f, 0f, 200f);
	}
	gl.glEnd();

	gl.glEnable(GL.GL_LIGHTING);
	//		gl.glDisable(GL.GL_COLOR_MATERIAL);
    }

    public String getMapa() {
	//TODO Implementar
	return null;
    }
}
