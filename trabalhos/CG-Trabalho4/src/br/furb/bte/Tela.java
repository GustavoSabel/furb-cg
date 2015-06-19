package br.furb.bte;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.media.opengl.DebugGL;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.glu.GLU;
import br.furb.bte.objetos.Arena;
import br.furb.bte.objetos.Cor;
import br.furb.bte.objetos.Moto;
import br.furb.bte.objetos.Mundo;
import com.sun.opengl.util.GLUT;
import com.sun.opengl.util.j2d.TextRenderer;

public class Tela extends GLCanvas implements GLEventAdapter {

    private class RenderLoop extends Thread {

	@Override
	public void run() {
	    while (true) {
		try {
		    if (animando || jogando || executandoPasso) {
			System.out.println("Tela.RenderLoop.run()");
			if (jogando) {
			    executarComportamentos();
			}
			glDrawable.display();
			Thread.sleep(50);
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


    private static final long serialVersionUID = 1L;
    private static final int NEAR = 1;
    private static final int FAR = 2000;
    private static final TextRenderer TEXT_RENDERER = new TextRenderer(new Font("SansSerif", Font.BOLD, 18));
    private static final int TAMANHO_ARENA = 500;

    // ========== RECURSOS DO CANVAS ==========
    private int largura = 800;
    private int altura = 600;
    private GL gl;
    private GLU glu;
    private GLAutoDrawable glDrawable;
    private boolean atualizarVisualizacao;
    private boolean perspectiveMode = true;
    private final float[] posicaoLuz = { 50, 50, 100, 0 };
    // ========== OBJETOS GRÃ�FICOS ==========
    private Mundo mundo;
    private Moto moto1;
    private Moto moto2;
    private Arena arena;
    private Camera camera;

    // ========== CONTROLES DE EXECUÃ‡ÃƒO ==========
    private EstadoJogo estadoJogo;
    private boolean jogando;
    private boolean animando = true;
    private boolean executandoPasso;

    private final RenderLoop renderLoop;

    public Tela() {
	setPreferredSize(new Dimension(largura, altura));
	renderLoop = new RenderLoop();
	addGLEventListener(this);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
	glDrawable = drawable;
	gl = drawable.getGL();

	gl.glEnable(GL.GL_DEPTH_TEST);
	gl.glEnable(GL.GL_CULL_FACE);

	glu = new GLU();
	glDrawable.setGL(new DebugGL(gl));
	gl.glClearColor(0f, 0f, 0f, 1.0f);

	mundo = new Mundo();

	camera = new Camera(this);
	reset();

	addKeyListener(new KeyAdapter() {

	    @Override
	    public void keyPressed(KeyEvent e) {
		if (trataControleMotos(e) || trataControleJogo(e) || trataControleCenario(e)) {
		    render();
		}
	    }
	});
	addComponentListener(new ComponentAdapter() {

	    @Override
	    public void componentResized(ComponentEvent e) {
		if (arena != null) {
		    render();
		}
	    }
	});
	renderLoop.start();
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
	gl.glEnable(GL.GL_LIGHT0);
	gl.glEnable(GL.GL_LIGHTING);
	// gl.glShadeModel(GL.GL_SMOOTH);
	gl.glShadeModel(GL.GL_FLAT);
	// gl.glEnable(GL.GL_COLOR_MATERIAL);
	gl.glColorMaterial(GL.GL_FRONT, GL.GL_AMBIENT_AND_DIFFUSE);
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
		    TEXT_RENDERER.draw("EspaÃ§o para continuar", meioLargura - 80, meioAltura - 20);
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

    private void alterarEstadoJogo(EstadoJogo novoEstado) {
	this.estadoJogo = novoEstado;
	render();
    }

    private void alterarExecucao(boolean executar) {
	this.jogando = executar;
	render();
    }

    private void reset() {
	animando = false;

	mundo.removerTodosFilhos();
	arena = new Arena(TAMANHO_ARENA, TAMANHO_ARENA);
	moto1 = new Moto(arena.getBBox().getMenorX() + 50, 0, new Cor(1, 0, 0), gl);
//	camera.seguirMoto(moto1);
	camera.seguirMoto(null);
	arena.addFilho(moto1);
	arena.addFilho(moto1.getRastro());

	moto2 = new Moto(arena.getBBox().getMaiorX() - 50, 0, new Cor(0, 1, 0), gl);
	moto2.girar(180);
	arena.addFilho(moto2);
	arena.addFilho(moto2.getRastro());

	//		Moto moto3 = new Moto(50, 50, new Cor(1, 0, 0), gl);
	//		
	//		arena.addFilho(moto3);

	mundo.addFilho(arena);
	jogando = false;
	estadoJogo = null;

	animando = true;
	render();
    }

    private boolean trataControleMotos(KeyEvent e) {
	if (!jogando && !e.isControlDown()) {
	    return false;
	}
	int direita = 90;
	int esquerda = -90;
	switch (e.getKeyCode()) {
	// case KeyEvent.VK_W:
	// this.moto1.setAngulo(Moto.CIMA);
	// break;
	// case KeyEvent.VK_UP:
	// this.moto2.setAngulo(Moto.CIMA);
	// break;
	// case KeyEvent.VK_D:
	// this.moto1.setAngulo(Moto.DIREITA);
	// break;
	// case KeyEvent.VK_RIGHT:
	// this.moto2.setAngulo(Moto.DIREITA);
	// break;
	// case KeyEvent.VK_S:
	// this.moto1.setAngulo(Moto.BAIXO);
	// break;
	// case KeyEvent.VK_DOWN:
	// this.moto2.setAngulo(Moto.BAIXO);
	// break;
	// case KeyEvent.VK_A:
	// this.moto1.setAngulo(Moto.ESQUERDA);
	// break;
	// case KeyEvent.VK_LEFT:
	// this.moto2.setAngulo(Moto.ESQUERDA);
	// break;
	    case KeyEvent.VK_D:
		moto1.girar(direita);
		break;
	    case KeyEvent.VK_RIGHT:
		moto2.girar(direita);
		break;
	    case KeyEvent.VK_A:
		moto1.girar(esquerda);
		break;
	    case KeyEvent.VK_LEFT:
		moto2.girar(esquerda);
		break;
	    default:
		return false;
	}
	return true;
    }

    private boolean trataControleJogo(KeyEvent e) {
	switch (e.getKeyCode()) {
	    case KeyEvent.VK_SPACE:
		if (!jogando && e.isControlDown()) {
		    executarComportamentos();
		    executandoPasso = true;
		} else if (estadoJogo == null) {
		    alterarExecucao(!jogando);
		}
		break;
	    case KeyEvent.VK_R:
		reset();

		break;
	    default:
		return false;
	}
	return true;
    }

    private boolean trataControleCenario(KeyEvent e) {
	if (e.getKeyCode() == KeyEvent.VK_1) {
	    perspectiveMode = !perspectiveMode;
	    atualizarVisualizacao = true;
	    return true;
	}
	return false;
    }

    private void executarComportamentos() {
	moto1.mover();
	moto2.mover();

	boolean teveColisao1 = false;
	boolean teveColisao2 = false;
	teveColisao1 = verificaColisaoMoto(moto1);
	teveColisao2 = verificaColisaoMoto(moto2);
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

    /**
     * Desenha os eixos do Sistema de ReferÃªncia Universal
     */
    public static void desenhaSRU(GL gl) {
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
    }

}
