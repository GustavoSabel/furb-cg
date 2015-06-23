package br.furb.bte.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import br.furb.bte.controle.Controlador;
import br.furb.bte.controle.ControladorLocal;
import br.furb.bte.controle.ia.ControladorIA;
import br.furb.bte.ui.UIUtils.SupportedLookAndFeel;

@SuppressWarnings("serial")
public class Launcher extends JFrame {

    private static final Font FONT_HIGH = new Font("Tahoma", Font.BOLD, 15);
    private JButton btnRede;
    private final DialogoRede dialogoRede;
    private final DialogoComandos dialogoComandos;
    private JButton btnLocal;
    private JButton btnComandos;
    private JButton btnLocalIA;
    

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
	UIUtils.changeLookAndFeelIfPossible(SupportedLookAndFeel.SYSTEM_DEFAULT);
	try {
	    Launcher launcher = new Launcher();
	    launcher.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	    UIUtils.centerOnScreen(launcher);
	    launcher.setVisible(true);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * Create the dialog.
     */
    public Launcher() {
	build();

	dialogoComandos = new DialogoComandos(this);
	dialogoRede = new DialogoRede(this);
	UIUtils.centerOnScreen(dialogoComandos);
	UIUtils.centerOnScreen(dialogoRede);

	assignEvents();
    }

    private void assignEvents() {
	btnRede.addActionListener(event -> {
	    setVisible(false);
	    dialogoRede.setVisible(true);
	});

	btnLocal.addActionListener(event -> {
	    launch(new ControladorLocal());
	});

	btnComandos.addActionListener(event -> {
	    dialogoComandos.setVisible(true);
	});
	
	btnLocalIA.addActionListener(event -> {
	    launch(new ControladorIA());
	});
    }

    @Override
    public void dispose() {
	dialogoComandos.dispose();
	dialogoRede.dispose();
	super.dispose();
    }

    private void build() {
	setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	setResizable(false);
	setTitle("..::TRON::..");
	setBounds(100, 100, 450, 417);
	getContentPane().setLayout(new BorderLayout());
	{
	    JPanel panelBanner = new JPanel();
	    getContentPane().add(panelBanner, BorderLayout.NORTH);
	    FlowLayout flowLayout = (FlowLayout) panelBanner.getLayout();
	    flowLayout.setVgap(0);
	    flowLayout.setHgap(0);
	    {
		JLabel lblTron = new JLabel("");
		lblTron.setIcon(new ImageIcon("res/launcher_bg.jpg"));
		panelBanner.add(lblTron);
	    }
	}
	JPanel panelCenter = new JPanel();
	getContentPane().add(panelCenter, BorderLayout.CENTER);
	panelCenter.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Modo de jogo",
		TitledBorder.CENTER, TitledBorder.TOP, FONT_HIGH, Color.LIGHT_GRAY));
	panelCenter.setBackground(Color.DARK_GRAY);
	GridBagLayout gbl_panelCenter = new GridBagLayout();
	gbl_panelCenter.columnWidths = new int[] { 0, 0, 0, 0, 0 };
	gbl_panelCenter.rowHeights = new int[] { 0, 0, 0, 0, 0, 0 };
	gbl_panelCenter.columnWeights = new double[] { 1.0, 0.0, 0.0, 1.0, Double.MIN_VALUE };
	gbl_panelCenter.rowWeights = new double[] { 1.0, 1.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
	panelCenter.setLayout(gbl_panelCenter);
	{
	    btnLocal = new JButton("Multiplayer");
	    btnLocal.setFont(FONT_HIGH);
	    btnLocal.setBackground(Color.GRAY);
	    GridBagConstraints gbc_btnLocal = new GridBagConstraints();
	    gbc_btnLocal.fill = GridBagConstraints.VERTICAL;
	    gbc_btnLocal.gridheight = 2;
	    gbc_btnLocal.insets = new Insets(0, 0, 5, 5);
	    gbc_btnLocal.gridx = 1;
	    gbc_btnLocal.gridy = 1;
	    panelCenter.add(btnLocal, gbc_btnLocal);
	}
	{
	    btnLocalIA = new JButton("Singleplayer");
	    btnLocalIA.setFont(FONT_HIGH);
	    btnLocalIA.setBackground(Color.GRAY);
	    GridBagConstraints gbc_btnRede = new GridBagConstraints();
	    gbc_btnRede.gridheight = 2;
	    gbc_btnRede.fill = GridBagConstraints.BOTH;
	    gbc_btnRede.insets = new Insets(0, 0, 5, 5);
	    gbc_btnRede.gridx = 2;
	    gbc_btnRede.gridy = 1;
	    panelCenter.add(btnLocalIA, gbc_btnRede);
	}
	{
	    btnRede = new JButton("Rede");
	    btnRede.setFont(FONT_HIGH);
	    btnRede.setBackground(Color.GRAY);
	    GridBagConstraints gbc_btnRede = new GridBagConstraints();
	    gbc_btnRede.gridheight = 2;
	    gbc_btnRede.fill = GridBagConstraints.BOTH;
	    gbc_btnRede.insets = new Insets(0, 0, 5, 5);
	    gbc_btnRede.gridx = 3;
	    gbc_btnRede.gridy = 1;
	    panelCenter.add(btnRede, gbc_btnRede);
	}
	{
	    btnComandos = new JButton("Comandos");
	    btnComandos.setBackground(Color.GRAY);
	    GridBagConstraints gbc_btnComandos = new GridBagConstraints();
	    gbc_btnComandos.gridwidth = 2;
	    gbc_btnComandos.insets = new Insets(0, 0, 0, 5);
	    gbc_btnComandos.gridx = 1;
	    gbc_btnComandos.gridy = 4;
	    panelCenter.add(btnComandos, gbc_btnComandos);
	}
    }

    public void launch(Controlador controlador) {
	setVisible(false);

	CanvasFrame frame = new CanvasFrame(getTitle(), controlador);
	UIUtils.centerOnScreen(frame);
	frame.setVisible(true);

	dispose();
    }

}
