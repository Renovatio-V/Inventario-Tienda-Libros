package Renovatio.Tienda_Libros.vista;

import Renovatio.Tienda_Libros.modelo.Libro;
import Renovatio.Tienda_Libros.servicio.LibroServicio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LibroForm extends JFrame {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final System.Logger LOGGER = System.getLogger(LibroForm.class.getName());

    private final LibroServicio libroServicio;

    // Contenedores y Modelos
    private JPanel panel;
    private JPanel centerCardPanel;
    private CardLayout cardLayout;
    private JList<Libro> galeriaLibros;
    private DefaultListModel<Libro> galeriaModelo;
    private JTable tablaLibros;
    private DefaultTableModel tablaModeloLibros;

    // Estado de datos y filtros
    private List<Libro> catalogoCompleto = new ArrayList<>();
    private String letraFiltroActiva = "TODOS";
    private final List<JButton> botonesAlfabeto = new ArrayList<>();

    // Campos del Inspector
    private JTextField idTexto;
    private JTextField libroTexto;
    private JTextField autorTexto;
    private JTextField precioTexto;
    private JTextField inventarioTexto;
    private JTextField buscarTexto;

    // Autocompletado Inteligente (Título y Autor)
    private JPopupMenu popupSugerencias;
    private Timer debounceTituloTimer;
    private Timer debounceAutorTimer;
    private boolean actualizandoCamposProgramaticamente = false;
    private long revisionSugerencias;
    private JButton btnDetectarAutor;

    // Preview en el Inspector
    private JLabel previewCoverLabel;
    private JLabel previewTituloLabel;
    private JLabel previewAutorLabel;
    private JLabel previewMetaLabel;
    private JLabel previewStatusBadge;

    // Botones CRUD y Vistas
    private JButton agregarButton;
    private JButton modificarButton;
    private JButton eliminarButton;
    private JButton limpiarButton;
    private JButton btnVistaPortadas;
    private JButton btnVistaTabla;

    // Telemetría Header
    private JLabel badgeTotalLibros;
    private JLabel kpiUnidades;
    private JLabel kpiValor;
    private JLabel kpiAlertas;

    // Caché y Cliente HTTP
    private final Map<String, ImageIcon> cachePortadas = new ConcurrentHashMap<>();
    private final Map<String, Boolean> descargasEnCurso = new ConcurrentHashMap<>();
    private final File carpetaCache;
    private final HttpClient httpClient;

    // Paleta Minimalista Negro Obsidiana
    private static final Color BG_OBSIDIAN   = new Color(8, 8, 10);       // #08080A
    private static final Color BG_SIDEBAR    = new Color(13, 13, 16);     // #0D0D10
    private static final Color BG_CARD       = new Color(17, 17, 21);     // #111115
    private static final Color BG_INPUT      = new Color(22, 22, 27);     // #16161B
    private static final Color BORDER_SUBTLE = new Color(34, 34, 42);     // #22222A
    private static final Color TEXT_WHITE    = new Color(245, 245, 247);  // #F5F5F7
    private static final Color TEXT_MUTED    = new Color(138, 138, 150);  // #8A8A96
    private static final Color TEXT_DIM      = new Color(82, 82, 94);     // #52525E
    private static final Color ACCENT_CYAN   = new Color(56, 189, 248);   // #38BDF8
    private static final Color STATUS_OK     = new Color(52, 211, 153);   // #34D399
    private static final Color STATUS_WARN   = new Color(251, 191, 36);   // #FBBF24
    private static final Color STATUS_DANGER = new Color(248, 113, 113);  // #F87171

    private static final Font FONT_BRAND = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_H1    = new Font("Segoe UI", Font.BOLD, 24);
    private static final Font FONT_BODY  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD  = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font FONT_MONO  = new Font("Consolas", Font.BOLD, 12);

    private static final int COVER_W = 148;
    private static final int COVER_H = 204;

    private record SugerenciaLibro(String titulo, String autor, String thumbnailUrl) {}

    public LibroForm(LibroServicio libroServicio) {
        this.libroServicio = libroServicio;
        this.carpetaCache = new File(System.getProperty("user.home"), ".tienda_libros_covers");
        if (!carpetaCache.exists()) {
            carpetaCache.mkdirs();
        }
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        iniciarForma();
        configurarAutocompletado();
        configurarEventos();
        actualizarEstadoBotonesFormulario(false);
    }

    private void iniciarForma() {
        idTexto = new JTextField("");
        idTexto.setVisible(false);

        panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BG_OBSIDIAN);

        panel.add(crearSidebarInspector(), BorderLayout.WEST);
        panel.add(crearAreaBiblioteca(), BorderLayout.CENTER);

        listarLibros();

        setTitle("Tienda de Libros // Obsidian Library Studio");
        setContentPane(panel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 780);
        setMinimumSize(new Dimension(1080, 680));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel crearSidebarInspector() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 16));
        sidebar.setBackground(BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(330, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_SUBTLE),
                new EmptyBorder(22, 22, 22, 22)
        ));

        JPanel topBox = new JPanel(new BorderLayout(0, 14));
        topBox.setOpaque(false);

        JPanel brandRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brandRow.setOpaque(false);
        JLabel logo = new JLabel("renovatio.");
        logo.setFont(FONT_BRAND);
        logo.setForeground(TEXT_WHITE);

        JLabel tag = new JLabel(" LIBRARY ");
        tag.setFont(new Font("Segoe UI", Font.BOLD, 10));
        tag.setForeground(ACCENT_CYAN);
        tag.setBorder(new LineBorder(new Color(56, 189, 248, 90), 1, true));

        brandRow.add(logo);
        brandRow.add(tag);

        JPanel previewCard = new JPanel(new BorderLayout(12, 0));
        previewCard.setBackground(BG_OBSIDIAN);
        previewCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_SUBTLE, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));

        previewCoverLabel = new JLabel();
        previewCoverLabel.setPreferredSize(new Dimension(68, 96));
        previewCoverLabel.setIcon(escalarIcono(generarPortadaEditorial("Nuevo Libro", "Autor", 0), 68, 96));

        JPanel metaBox = new JPanel();
        metaBox.setOpaque(false);
        metaBox.setLayout(new BoxLayout(metaBox, BoxLayout.Y_AXIS));

        previewStatusBadge = new JLabel("● Auto-Cover & Autor Web");
        previewStatusBadge.setFont(FONT_SMALL);
        previewStatusBadge.setForeground(STATUS_OK);

        previewTituloLabel = new JLabel("Modo Nuevo Registro");
        previewTituloLabel.setFont(FONT_BOLD);
        previewTituloLabel.setForeground(TEXT_WHITE);

        previewAutorLabel = new JLabel("Escribe para autocompletar");
        previewAutorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        previewAutorLabel.setForeground(TEXT_MUTED);

        previewMetaLabel = new JLabel("SKU #NUEVO  •  $0.00");
        previewMetaLabel.setFont(FONT_MONO);
        previewMetaLabel.setForeground(ACCENT_CYAN);

        metaBox.add(previewStatusBadge);
        metaBox.add(Box.createVerticalStrut(6));
        metaBox.add(previewTituloLabel);
        metaBox.add(Box.createVerticalStrut(2));
        metaBox.add(previewAutorLabel);
        metaBox.add(Box.createVerticalStrut(10));
        metaBox.add(previewMetaLabel);

        previewCard.add(previewCoverLabel, BorderLayout.WEST);
        previewCard.add(metaBox, BorderLayout.CENTER);

        topBox.add(brandRow, BorderLayout.NORTH);
        topBox.add(previewCard, BorderLayout.CENTER);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        libroTexto      = crearCampoTexto();
        autorTexto      = crearCampoTexto();
        precioTexto     = crearCampoTexto();
        inventarioTexto = crearCampoTexto();

        libroTexto.setToolTipText("Escribe el título para ver sugerencias y autocompletar el autor");
        autorTexto.setToolTipText("Escribe el autor o haz clic en '⚡ Buscar Autor' según el título");

        agregarCampoInspector(formPanel, gbc, 0, "TÍTULO DEL LIBRO (AUTOCOMPLETADO)", libroTexto, null);

        btnDetectarAutor = new JButton("⚡ Buscar Autor");
        btnDetectarAutor.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnDetectarAutor.setForeground(ACCENT_CYAN);
        btnDetectarAutor.setBackground(BG_OBSIDIAN);
        btnDetectarAutor.setFocusPainted(false);
        btnDetectarAutor.setBorder(new LineBorder(new Color(56, 189, 248, 80), 1, true));
        btnDetectarAutor.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDetectarAutor.setToolTipText("Buscar autores oficiales para el título escrito arriba");

        agregarCampoInspector(formPanel, gbc, 2, "AUTOR OFICIAL", autorTexto, btnDetectarAutor);
        agregarCampoInspector(formPanel, gbc, 4, "PRECIO (USD)", precioTexto, null);
        agregarCampoInspector(formPanel, gbc, 6, "EXISTENCIAS", inventarioTexto, null);

        gbc.gridy = 8;
        gbc.weighty = 1.0;
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        formPanel.add(spacer, gbc);

        JPanel actionsBox = new JPanel(new BorderLayout(0, 8));
        actionsBox.setOpaque(false);

        agregarButton = crearBoton("+ Agregar con Portada", TEXT_WHITE, BG_OBSIDIAN, TEXT_WHITE);
        agregarButton.setPreferredSize(new Dimension(0, 40));

        JPanel midButtons = new JPanel(new GridLayout(1, 2, 8, 0));
        midButtons.setOpaque(false);
        modificarButton = crearBoton("Modificar", BG_INPUT, TEXT_WHITE, BORDER_SUBTLE);
        eliminarButton  = crearBoton("Eliminar", new Color(45, 18, 24), STATUS_DANGER, new Color(95, 32, 42));
        midButtons.add(modificarButton);
        midButtons.add(eliminarButton);

        limpiarButton = crearBoton("Limpiar Selección / Nuevo", BG_OBSIDIAN, TEXT_MUTED, BORDER_SUBTLE);
        limpiarButton.setPreferredSize(new Dimension(0, 32));

        actionsBox.add(agregarButton, BorderLayout.NORTH);
        actionsBox.add(midButtons, BorderLayout.CENTER);
        actionsBox.add(limpiarButton, BorderLayout.SOUTH);

        sidebar.add(topBox, BorderLayout.NORTH);
        sidebar.add(formPanel, BorderLayout.CENTER);
        sidebar.add(actionsBox, BorderLayout.SOUTH);

        return sidebar;
    }

    private void agregarCampoInspector(JPanel container, GridBagConstraints gbc, int row,
                                       String labelTxt, JTextField field, JButton extraBtn) {
        gbc.gridy = row;
        gbc.weighty = 0;
        gbc.insets = new Insets(row == 0 ? 0 : 10, 0, 4, 0);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        JLabel lbl = new JLabel(labelTxt);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(TEXT_MUTED);
        headerRow.add(lbl, BorderLayout.WEST);
        if (extraBtn != null) {
            headerRow.add(extraBtn, BorderLayout.EAST);
        }
        container.add(headerRow, gbc);

        gbc.gridy = row + 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        container.add(field, gbc);
    }

    private JPanel crearAreaBiblioteca() {
        JPanel mainArea = new JPanel(new BorderLayout(0, 16));
        mainArea.setBackground(BG_OBSIDIAN);
        mainArea.setBorder(new EmptyBorder(22, 28, 22, 28));

        JPanel northStack = new JPanel();
        northStack.setOpaque(false);
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));

        JPanel searchRow = new JPanel(new BorderLayout(16, 0));
        searchRow.setOpaque(false);

        buscarTexto = crearCampoTexto();
        buscarTexto.setPreferredSize(new Dimension(380, 38));
        buscarTexto.setToolTipText("Buscar por título, autor o SKU en tiempo real...");

        JPanel searchWrapper = new JPanel(new BorderLayout(8, 0));
        searchWrapper.setBackground(BG_INPUT);
        searchWrapper.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_SUBTLE, 1, true),
                new EmptyBorder(0, 14, 0, 8)
        ));
        JLabel searchIcon = new JLabel("FILTRAR:");
        searchIcon.setFont(FONT_SMALL);
        searchIcon.setForeground(TEXT_MUTED);
        buscarTexto.setBorder(new EmptyBorder(6, 6, 6, 6));
        searchWrapper.add(searchIcon, BorderLayout.WEST);
        searchWrapper.add(buscarTexto, BorderLayout.CENTER);

        JPanel viewSwitcher = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        viewSwitcher.setOpaque(false);
        btnVistaPortadas = crearBoton("Portadas (Grid)", TEXT_WHITE, BG_OBSIDIAN, TEXT_WHITE);
        btnVistaPortadas.setPreferredSize(new Dimension(135, 36));
        btnVistaTabla = crearBoton("Vista Tabla", BG_INPUT, TEXT_MUTED, BORDER_SUBTLE);
        btnVistaTabla.setPreferredSize(new Dimension(115, 36));
        viewSwitcher.add(btnVistaPortadas);
        viewSwitcher.add(btnVistaTabla);

        searchRow.add(searchWrapper, BorderLayout.WEST);
        searchRow.add(viewSwitcher, BorderLayout.EAST);

        JPanel bannerRow = new JPanel(new GridLayout(1, 4, 12, 0));
        bannerRow.setOpaque(false);

        JPanel libCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        libCard.setBackground(BG_CARD);
        libCard.setBorder(new LineBorder(BORDER_SUBTLE, 1, true));
        JLabel lblMiBib = new JLabel("Mi Biblioteca");
        lblMiBib.setFont(FONT_H1);
        lblMiBib.setForeground(TEXT_WHITE);
        badgeTotalLibros = new JLabel(" 0 ");
        badgeTotalLibros.setFont(FONT_BOLD);
        badgeTotalLibros.setForeground(ACCENT_CYAN);
        badgeTotalLibros.setBorder(new LineBorder(BORDER_SUBTLE, 1, true));
        libCard.add(lblMiBib);
        libCard.add(badgeTotalLibros);

        kpiUnidades = new JLabel("0 uds");
        kpiValor    = new JLabel("$0.00");
        kpiAlertas  = new JLabel("0 alertas");

        bannerRow.add(libCard);
        bannerRow.add(crearMiniKpi("STOCK TOTAL", kpiUnidades, TEXT_WHITE));
        bannerRow.add(crearMiniKpi("VALORIZACIÓN", kpiValor, STATUS_OK));
        bannerRow.add(crearMiniKpi("BAJO STOCK (<=5)", kpiAlertas, STATUS_WARN));

        JPanel alphaBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 4));
        alphaBar.setOpaque(false);
        alphaBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_SUBTLE));

        String[] letras = {"TODOS", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L",
                           "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
        for (String letra : letras) {
            JButton btnLetra = new JButton(letra);
            btnLetra.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnLetra.setMargin(new Insets(2, 6, 2, 6));
            btnLetra.setFocusPainted(false);
            btnLetra.setContentAreaFilled(false);
            btnLetra.setBorder(new EmptyBorder(4, 6, 4, 6));
            btnLetra.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnLetra.setForeground(letra.equals("TODOS") ? ACCENT_CYAN : TEXT_DIM);

            btnLetra.addActionListener(e -> {
                letraFiltroActiva = letra;
                for (JButton b : botonesAlfabeto) {
                    b.setForeground(b.getText().equals(letraFiltroActiva) ? ACCENT_CYAN : TEXT_DIM);
                }
                aplicarFiltros();
            });
            botonesAlfabeto.add(btnLetra);
            alphaBar.add(btnLetra);
        }

        northStack.add(searchRow);
        northStack.add(Box.createVerticalStrut(14));
        northStack.add(bannerRow);
        northStack.add(Box.createVerticalStrut(10));
        northStack.add(alphaBar);

        cardLayout = new CardLayout();
        centerCardPanel = new JPanel(cardLayout);
        centerCardPanel.setOpaque(false);

        galeriaModelo = new DefaultListModel<>();
        galeriaLibros = new JList<>(galeriaModelo);
        galeriaLibros.setBackground(BG_OBSIDIAN);
        galeriaLibros.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        galeriaLibros.setVisibleRowCount(0);
        galeriaLibros.setFixedCellWidth(196);
        galeriaLibros.setFixedCellHeight(292);
        galeriaLibros.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        galeriaLibros.setCellRenderer(new PortadaCardRenderer());

        JScrollPane scrollGaleria = new JScrollPane(galeriaLibros);
        scrollGaleria.setBorder(new EmptyBorder(0, 0, 0, 0));
        scrollGaleria.getViewport().setBackground(BG_OBSIDIAN);
        scrollGaleria.getVerticalScrollBar().setUnitIncrement(18);

        tablaModeloLibros = new DefaultTableModel(0, 5) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaModeloLibros.setColumnIdentifiers(new String[]{"SKU", "TÍTULO", "AUTOR", "PRECIO", "STOCK"});
        tablaLibros = new JTable(tablaModeloLibros);
        estilizarTablaMinimalista();

        JScrollPane scrollTabla = new JScrollPane(tablaLibros);
        scrollTabla.getViewport().setBackground(BG_CARD);
        scrollTabla.setBorder(new LineBorder(BORDER_SUBTLE, 1));

        centerCardPanel.add(scrollGaleria, "PORTADAS");
        centerCardPanel.add(scrollTabla, "TABLA");

        mainArea.add(northStack, BorderLayout.NORTH);
        mainArea.add(centerCardPanel, BorderLayout.CENTER);

        return mainArea;
    }

    private JPanel crearMiniKpi(String label, JLabel valueLabel, Color valColor) {
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_SUBTLE, 1, true),
                new EmptyBorder(10, 16, 10, 16)
        ));
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(TEXT_MUTED);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        valueLabel.setForeground(valColor);

        card.add(lbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    // ==================== MOTOR DE AUTOCOMPLETADO INTELIGENTE (TÍTULO <-> AUTOR) ====================
    private void configurarAutocompletado() {
        popupSugerencias = new JPopupMenu();
        popupSugerencias.setBackground(BG_CARD);
        popupSugerencias.setBorder(new LineBorder(ACCENT_CYAN, 1));
        popupSugerencias.setFocusable(false); // No roba el foco mientras escribes

        debounceTituloTimer = new Timer(340, e -> buscarSugerenciasWeb(true));
        debounceTituloTimer.setRepeats(false);

        debounceAutorTimer = new Timer(340, e -> buscarSugerenciasWeb(false));
        debounceAutorTimer.setRepeats(false);

        libroTexto.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { dispararDebounceTitulo(); }
            @Override
            public void removeUpdate(DocumentEvent e) { dispararDebounceTitulo(); }
            @Override
            public void changedUpdate(DocumentEvent e) { dispararDebounceTitulo(); }
        });

        autorTexto.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { dispararDebounceAutor(); }
            @Override
            public void removeUpdate(DocumentEvent e) { dispararDebounceAutor(); }
            @Override
            public void changedUpdate(DocumentEvent e) { dispararDebounceAutor(); }
        });

        btnDetectarAutor.addActionListener(e -> {
            String titulo = libroTexto.getText().trim();
            if (titulo.length() < 2) {
                mostrarMensaje("Escribe primero el título del libro arriba para buscar su autor oficial.");
                libroTexto.requestFocusInWindow();
                return;
            }
            revisionSugerencias++;
            buscarSugerenciasWeb(false);
        });
    }

    private void dispararDebounceTitulo() {
        if (actualizandoCamposProgramaticamente) return;
        revisionSugerencias++;
        String texto = libroTexto.getText().trim();
        if (texto.length() < 2) {
            popupSugerencias.setVisible(false);
            return;
        }
        debounceTituloTimer.restart();
    }

    private void dispararDebounceAutor() {
        if (actualizandoCamposProgramaticamente) return;
        revisionSugerencias++;
        String textoAutor = autorTexto.getText().trim();
        String textoTitulo = libroTexto.getText().trim();
        if (textoAutor.length() < 2 && textoTitulo.length() < 2) {
            popupSugerencias.setVisible(false);
            return;
        }
        debounceAutorTimer.restart();
    }

    private void buscarSugerenciasWeb(boolean desdeCampoTitulo) {
        String tituloInput = libroTexto.getText().trim();
        String autorInput = autorTexto.getText().trim();
        String tituloConsulta;
        String autorConsulta;

        if (desdeCampoTitulo) {
            if (tituloInput.length() < 2) return;
            tituloConsulta = tituloInput;
            autorConsulta = "";
        } else {
            tituloConsulta = tituloInput;
            autorConsulta = autorInput;
            if (tituloConsulta.length() < 2 && autorConsulta.length() < 2) return;
        }

        JTextField campoAncla = desdeCampoTitulo ? libroTexto : autorTexto;
        long revision = ++revisionSugerencias;

        CompletableFuture.supplyAsync(() -> consultarSugerencias(tituloConsulta, autorConsulta))
                .thenAccept(sugerencias -> SwingUtilities.invokeLater(() -> {
                    if (revision != revisionSugerencias) return;
                    if (sugerencias.isEmpty()) {
                        popupSugerencias.setVisible(false);
                        return;
                    }
                    mostrarPopupSugerencias(sugerencias, campoAncla, desdeCampoTitulo);
                }));
    }

    private List<SugerenciaLibro> consultarSugerencias(String titulo, String autor) {
        List<SugerenciaLibro> sugerencias = consultarSugerenciasGoogleBooks(titulo, autor);
        return sugerencias.isEmpty() ? consultarSugerenciasOpenLibrary(titulo, autor) : sugerencias;
    }

    private List<SugerenciaLibro> consultarSugerenciasGoogleBooks(String titulo, String autor) {
        List<SugerenciaLibro> resultados = new ArrayList<>();
        try {
            StringBuilder query = new StringBuilder();
            if (!titulo.isBlank()) query.append("intitle:").append(titulo);
            if (!autor.isBlank()) {
                if (!query.isEmpty()) query.append(' ');
                query.append("inauthor:").append(autor);
            }
            String qEncoded = URLEncoder.encode(query.toString(), StandardCharsets.UTF_8);
            String url = "https://www.googleapis.com/books/v1/volumes?q=" + qEncoded + "&maxResults=5&printType=books";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "TiendaLibrosObsidian/2.5")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200 || resp.body() == null) {
                LOGGER.log(System.Logger.Level.WARNING, "Google Books respondió HTTP {0}; se intentará Open Library.", resp.statusCode());
                return resultados;
            }

            JsonNode items = JSON_MAPPER.readTree(resp.body()).path("items");
            for (JsonNode item : items) {
                JsonNode info = item.path("volumeInfo");
                String tituloLibro = info.path("title").asText("");
                String autores = unirAutores(info.path("authors"));
                String thumbnail = info.path("imageLinks").path("thumbnail").asText("")
                        .replace("http://", "https://");
                if (!tituloLibro.isBlank() && !autores.isBlank()) {
                    resultados.add(new SugerenciaLibro(tituloLibro, autores, thumbnail));
                    if (resultados.size() == 5) break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(System.Logger.Level.WARNING, "La búsqueda en Google Books fue interrumpida.", e);
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.WARNING, "No se pudo consultar Google Books; se intentará Open Library.", e);
        }
        return resultados;
    }

    private List<SugerenciaLibro> consultarSugerenciasOpenLibrary(String titulo, String autor) {
        List<SugerenciaLibro> resultados = new ArrayList<>();
        try {
            StringBuilder url = new StringBuilder("https://openlibrary.org/search.json?limit=5&fields=title,author_name");
            if (!titulo.isBlank()) url.append("&title=").append(URLEncoder.encode(titulo, StandardCharsets.UTF_8));
            if (!autor.isBlank()) url.append("&author=").append(URLEncoder.encode(autor, StandardCharsets.UTF_8));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .header("User-Agent", "TiendaLibrosObsidian/2.5")
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200 || resp.body() == null) {
                LOGGER.log(System.Logger.Level.WARNING, "Open Library respondió HTTP {0}.", resp.statusCode());
                return resultados;
            }

            JsonNode docs = JSON_MAPPER.readTree(resp.body()).path("docs");
            for (JsonNode doc : docs) {
                String tituloLibro = doc.path("title").asText("");
                String autores = unirAutores(doc.path("author_name"));
                if (!tituloLibro.isBlank() && !autores.isBlank()) {
                    resultados.add(new SugerenciaLibro(tituloLibro, autores, ""));
                    if (resultados.size() == 5) break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(System.Logger.Level.WARNING, "La búsqueda en Open Library fue interrumpida.", e);
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.WARNING, "No se pudo consultar Open Library.", e);
        }
        return resultados;
    }

    private String unirAutores(JsonNode autoresNode) {
        List<String> autores = new ArrayList<>();
        if (autoresNode.isArray()) {
            for (JsonNode autor : autoresNode) {
                String nombre = autor.asText("").trim();
                if (!nombre.isEmpty()) autores.add(nombre);
            }
        }
        return String.join(", ", autores);
    }

    private void mostrarPopupSugerencias(List<SugerenciaLibro> sugerencias, JTextField ancla, boolean desdeTitulo) {
        if (!ancla.isShowing()) return;

        popupSugerencias.setVisible(false);
        popupSugerencias.removeAll();

        JMenuItem headerItem = new JMenuItem(desdeTitulo
                ? "  SUGERENCIAS DE LIBROS (AUTOCOMPLETA EL AUTOR)"
                : "  AUTORES VERIFICADOS ENCONTRADOS");
        headerItem.setFont(new Font("Segoe UI", Font.BOLD, 10));
        headerItem.setForeground(ACCENT_CYAN);
        headerItem.setBackground(BG_OBSIDIAN);
        headerItem.setEnabled(false);
        popupSugerencias.add(headerItem);

        for (SugerenciaLibro sug : sugerencias) {
            String html = String.format(
                    "<html><div style='padding:3px 6px;'>" +
                    "<b style='color:#F5F5F7;font-size:10px;'>%s</b><br>" +
                    "<span style='color:#38BDF8;font-size:9px;'>Autor: %s</span>" +
                    "</div></html>",
                    escaparHtml(truncarSimple(sug.titulo(), 42)),
                    escaparHtml(truncarSimple(sug.autor(), 44))
            );

            JMenuItem item = new JMenuItem(html);
            item.setBackground(BG_CARD);
            item.setForeground(TEXT_WHITE);
            item.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_SUBTLE));
            item.setCursor(new Cursor(Cursor.HAND_CURSOR));

            item.addActionListener(e -> aplicarSugerenciaSeleccionada(sug));
            popupSugerencias.add(item);
        }

        popupSugerencias.setPopupSize(ancla.getWidth(), sugerencias.size() * 46 + 28);
        popupSugerencias.show(ancla, 0, ancla.getHeight() + 2);
    }

    private void aplicarSugerenciaSeleccionada(SugerenciaLibro sug) {
        actualizandoCamposProgramaticamente = true;
        libroTexto.setText(sug.titulo());
        autorTexto.setText(sug.autor());
        actualizandoCamposProgramaticamente = false;
        popupSugerencias.setVisible(false);

        previewTituloLabel.setText(sug.titulo());
        previewAutorLabel.setText(sug.autor());
        previewStatusBadge.setText("● Autor & Portada Verificados");
        previewStatusBadge.setForeground(STATUS_OK);

        // Pre-cargar portada instantáneamente en el Inspector
        Libro temp = new Libro(0, sug.titulo(), sug.autor(), 0.0, 0);
        ImageIcon cov = obtenerPortada(temp);
        previewCoverLabel.setIcon(new ImageIcon(cov.getImage().getScaledInstance(68, 96, Image.SCALE_SMOOTH)));

        // Si aún no ha escrito el precio, llevar el foco a Precio
        if (precioTexto.getText().trim().isEmpty()) {
            precioTexto.requestFocusInWindow();
        }
    }

    private String extraerCampoJson(String texto, String regex) {
        Matcher m = Pattern.compile(regex).matcher(texto);
        return m.find() ? m.group(1) : "";
    }

    private String decodificarJsonTexto(String input) {
        if (input == null) return "";
        String s = input.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\/", "/");
        Matcher m = Pattern.compile("\\\\u([0-9a-fA-F]{4})").matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            char ch = (char) Integer.parseInt(m.group(1), 16);
            m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(ch)));
        }
        m.appendTail(sb);
        return sb.toString().trim();
    }

    private String truncarSimple(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private String escaparHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ==================== RENDERIZADOR DE TARJETAS CON PORTADA ====================
    private class PortadaCardRenderer extends JPanel implements ListCellRenderer<Libro> {
        private Libro libroActual;
        private boolean seleccionado;
        private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

        public PortadaCardRenderer() {
            setOpaque(false);
        }

        @Override
        public java.awt.Component getListCellRendererComponent(JList<? extends Libro> list, Libro value,
                                                               int index, boolean isSelected, boolean cellHasFocus) {
            this.libroActual = value;
            this.seleccionado = isSelected;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (libroActual == null) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth() - 14;
            int h = getHeight() - 14;
            int x = 7;
            int y = 7;

            g2.setColor(BG_CARD);
            g2.fillRoundRect(x, y, w, h, 16, 16);

            g2.setColor(seleccionado ? ACCENT_CYAN : BORDER_SUBTLE);
            g2.setStroke(new BasicStroke(seleccionado ? 2.0f : 1.0f));
            g2.drawRoundRect(x, y, w, h, 16, 16);

            int covX = x + (w - COVER_W) / 2;
            int covY = y + 14;
            ImageIcon coverIcon = obtenerPortada(libroActual);
            if (coverIcon != null) {
                Shape clipAnterior = g2.getClip();
                g2.setClip(new RoundRectangle2D.Float(covX, covY, COVER_W, COVER_H, 10, 10));
                g2.drawImage(coverIcon.getImage(), covX, covY, COVER_W, COVER_H, null);
                g2.setClip(clipAnterior);

                g2.setColor(BORDER_SUBTLE);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(covX, covY, COVER_W, COVER_H, 10, 10);
            }

            double precio = libroActual.getPrecio() != null ? libroActual.getPrecio() : 0.0;
            String precioStr = currencyFormat.format(precio);
            g2.setFont(FONT_MONO);
            FontMetrics fmPrecio = g2.getFontMetrics();
            int pw = fmPrecio.stringWidth(precioStr) + 14;
            int px = covX + COVER_W - pw - 6;
            int py = covY + 6;

            g2.setColor(new Color(8, 8, 10, 220));
            g2.fillRoundRect(px, py, pw, 22, 8, 8);
            g2.setColor(TEXT_WHITE);
            g2.drawString(precioStr, px + 7, py + 15);

            int textY = covY + COVER_H + 20;
            g2.setFont(FONT_BOLD);
            g2.setColor(TEXT_WHITE);
            String titulo = truncarTexto(libroActual.getNombreLibro(), g2.getFontMetrics(), w - 24);
            g2.drawString(titulo, covX, textY);

            textY += 17;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.setColor(TEXT_MUTED);
            String autor = truncarTexto(libroActual.getAutor(), g2.getFontMetrics(), w - 24);
            g2.drawString(autor, covX, textY);

            int stock = libroActual.getExistencias() != null ? libroActual.getExistencias() : 0;
            Color badgeCol = stock <= 0 ? STATUS_DANGER : (stock <= 5 ? STATUS_WARN : STATUS_OK);
            Color badgeBg  = stock <= 0 ? new Color(248, 113, 113, 28)
                           : (stock <= 5 ? new Color(251, 191, 36, 28) : new Color(52, 211, 153, 24));

            int by = textY + 7;
            String stockTxt = "● " + stock + " uds";
            g2.setFont(FONT_SMALL);
            int bw = g2.getFontMetrics().stringWidth(stockTxt) + 16;

            g2.setColor(badgeBg);
            g2.fillRoundRect(covX, by, bw, 20, 8, 8);
            g2.setColor(badgeCol);
            g2.drawString(stockTxt, covX + 8, by + 14);

            g2.dispose();
        }
    }

    private String truncarTexto(String texto, FontMetrics fm, int maxWidth) {
        if (texto == null || texto.isEmpty()) return "Sin autor";
        if (fm.stringWidth(texto) <= maxWidth) return texto;
        String ellipsis = "...";
        int len = texto.length();
        while (len > 0 && fm.stringWidth(texto.substring(0, len) + ellipsis) > maxWidth) {
            len--;
        }
        return texto.substring(0, Math.max(0, len)) + ellipsis;
    }

    // ==================== MOTOR DE DESCARGA AUTOMÁTICA DE PORTADAS ====================
    private ImageIcon obtenerPortada(Libro libro) {
        String clave = generarClaveCache(libro.getNombreLibro(), libro.getAutor());
        if (cachePortadas.containsKey(clave)) {
            return cachePortadas.get(clave);
        }

        File archivoLocal = new File(carpetaCache, clave + ".jpg");
        if (archivoLocal.exists()) {
            try {
                BufferedImage img = ImageIO.read(archivoLocal);
                if (img != null) {
                    ImageIcon icono = escalarIcono(img, COVER_W, COVER_H);
                    cachePortadas.put(clave, icono);
                    return icono;
                }
            } catch (Exception ignored) {}
        }

        int seed = libro.getIdLibro() != null ? libro.getIdLibro() : Math.abs(clave.hashCode());
        BufferedImage fallback = generarPortadaEditorial(libro.getNombreLibro(), libro.getAutor(), seed);
        ImageIcon fallbackIcon = escalarIcono(fallback, COVER_W, COVER_H);
        cachePortadas.put(clave, fallbackIcon);

        if (!descargasEnCurso.containsKey(clave)) {
            descargasEnCurso.put(clave, true);
            descargarPortadaDeInternetAsync(libro.getNombreLibro(), libro.getAutor(), clave, archivoLocal);
        }

        return fallbackIcon;
    }

    private void descargarPortadaDeInternetAsync(String titulo, String autor, String clave, File destinoDisco) {
        if (titulo == null || titulo.isBlank()) return;

        CompletableFuture.runAsync(() -> {
            try {
                String termino = titulo.trim() + (autor != null && !autor.isBlank() ? " " + autor.trim() : "");
                String qEncoded = URLEncoder.encode(termino, StandardCharsets.UTF_8);

                String googleUrl = "https://www.googleapis.com/books/v1/volumes?q=" + qEncoded + "&maxResults=1";
                BufferedImage descargada = intentarDescargaGoogleBooks(googleUrl);

                if (descargada == null) {
                    String openLibUrl = "https://openlibrary.org/search.json?q=" + qEncoded + "&limit=1&fields=cover_i";
                    descargada = intentarDescargaOpenLibrary(openLibUrl);
                }

                if (descargada != null && descargada.getWidth() > 10) {
                    ImageIO.write(descargada, "jpg", destinoDisco);
                    ImageIcon nuevoIcono = escalarIcono(descargada, COVER_W, COVER_H);
                    cachePortadas.put(clave, nuevoIcono);

                    final BufferedImage imgFinal = descargada;
                    SwingUtilities.invokeLater(() -> {
                        galeriaLibros.repaint();
                        String actualClave = generarClaveCache(libroTexto.getText(), autorTexto.getText());
                        if (actualClave.equals(clave)) {
                            previewCoverLabel.setIcon(escalarIcono(imgFinal, 68, 96));
                        }
                    });
                }
            } catch (Exception ignored) {
            } finally {
                descargasEnCurso.remove(clave);
            }
        });
    }

    private BufferedImage intentarDescargaGoogleBooks(String apiUrl) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("User-Agent", "TiendaLibrosObsidian/2.5")
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body() != null) {
                Matcher m = Pattern.compile("\"thumbnail\"\\s*:\\s*\"([^\"]+)\"").matcher(resp.body());
                if (m.find()) {
                    String thumbUrl = m.group(1).replace("http://", "https://").replace("\\u0026", "&");
                    HttpRequest imgReq = HttpRequest.newBuilder()
                            .uri(URI.create(thumbUrl))
                            .header("User-Agent", "TiendaLibrosObsidian/2.5")
                            .timeout(Duration.ofSeconds(8))
                            .GET()
                            .build();
                    HttpResponse<InputStream> imgResp = httpClient.send(imgReq, HttpResponse.BodyHandlers.ofInputStream());
                    if (imgResp.statusCode() == 200) {
                        return ImageIO.read(imgResp.body());
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private BufferedImage intentarDescargaOpenLibrary(String apiUrl) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("User-Agent", "TiendaLibrosObsidian/2.5")
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body() != null) {
                Matcher matcher = Pattern.compile("\"cover_i\"\\s*:\\s*(\\d+)").matcher(resp.body());
                if (matcher.find()) {
                    String coverId = matcher.group(1);
                    String urlImagen = "https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg";
                    HttpRequest imgReq = HttpRequest.newBuilder()
                            .uri(URI.create(urlImagen))
                            .header("User-Agent", "TiendaLibrosObsidian/2.5")
                            .timeout(Duration.ofSeconds(10))
                            .GET()
                            .build();
                    HttpResponse<InputStream> imgResp = httpClient.send(imgReq, HttpResponse.BodyHandlers.ofInputStream());
                    if (imgResp.statusCode() == 200) {
                        return ImageIO.read(imgResp.body());
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private BufferedImage generarPortadaEditorial(String titulo, String autor, int seed) {
        BufferedImage img = new BufferedImage(COVER_W * 2, COVER_H * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = img.getWidth();
        int h = img.getHeight();

        Color[][] paletas = {
                {new Color(20, 44, 74), new Color(56, 189, 248)},
                {new Color(26, 30, 62), new Color(129, 140, 248)},
                {new Color(56, 20, 36), new Color(244, 63, 94)},
                {new Color(16, 44, 40), new Color(52, 211, 153)},
                {new Color(54, 34, 16), new Color(251, 191, 36)},
                {new Color(38, 22, 58), new Color(192, 132, 252)}
        };
        Color[] par = paletas[Math.abs(seed) % paletas.length];

        g2.setPaint(new GradientPaint(0, 0, par[0], w, h, new Color(10, 12, 18)));
        g2.fillRect(0, 0, w, h);

        g2.setColor(par[1]);
        g2.fillRect(28, 32, 64, 6);
        g2.setStroke(new BasicStroke(3f));
        g2.drawOval(w - 110, h - 110, 78, 78);

        g2.setColor(new Color(200, 210, 225));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.drawString("OBSIDIAN PRESS", 28, 62);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 24));
        String t = (titulo == null || titulo.isBlank()) ? "LIBRO" : titulo.toUpperCase();
        String[] palabras = t.split("\\s+");
        int yTxt = 105;
        StringBuilder linea = new StringBuilder();
        for (String p : palabras) {
            if (linea.length() + p.length() > 12 && linea.length() > 0) {
                g2.drawString(linea.toString(), 28, yTxt);
                yTxt += 30;
                linea = new StringBuilder(p).append(" ");
                if (yTxt > 220) break;
            } else {
                linea.append(p).append(" ");
            }
        }
        if (linea.length() > 0 && yTxt <= 220) {
            g2.drawString(linea.toString().trim(), 28, yTxt);
        }

        g2.setColor(new Color(180, 190, 205));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        String a = (autor == null || autor.isBlank()) ? "AUTOR EDITORIAL" : autor.toUpperCase();
        if (a.length() > 20) a = a.substring(0, 20) + "...";
        g2.drawString(a, 28, h - 28);

        g2.dispose();
        return img;
    }

    private ImageIcon escalarIcono(BufferedImage src, int w, int h) {
        Image scaled = src.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private String generarClaveCache(String titulo, String autor) {
        String base = ((titulo != null ? titulo : "") + "_" + (autor != null ? autor : "")).toLowerCase().trim();
        return base.replaceAll("[^a-z0-9]+", "_");
    }

    // ==================== ESTILOS Y CONTROLES ====================
    private void estilizarTablaMinimalista() {
        tablaLibros.setBackground(BG_CARD);
        tablaLibros.setForeground(TEXT_WHITE);
        tablaLibros.setFont(FONT_BODY);
        tablaLibros.setRowHeight(38);
        tablaLibros.setShowVerticalLines(false);
        tablaLibros.setGridColor(BORDER_SUBTLE);
        tablaLibros.setSelectionBackground(new Color(56, 189, 248, 35));
        tablaLibros.setSelectionForeground(TEXT_WHITE);
        tablaLibros.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader header = tablaLibros.getTableHeader();
        header.setBackground(BG_OBSIDIAN);
        header.setForeground(TEXT_MUTED);
        header.setFont(FONT_SMALL);
        header.setPreferredSize(new Dimension(0, 36));
        header.setBorder(new LineBorder(BORDER_SUBTLE, 1));

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                                                                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBorder(new EmptyBorder(0, 12, 0, 12));
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? BG_CARD : BG_SIDEBAR);
                }
                c.setForeground(column == 0 ? ACCENT_CYAN : TEXT_WHITE);
                return c;
            }
        };
        for (int i = 0; i < tablaLibros.getColumnCount(); i++) {
            tablaLibros.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    private JTextField crearCampoTexto() {
        JTextField field = new JTextField(15);
        field.setBackground(BG_INPUT);
        field.setForeground(TEXT_WHITE);
        field.setCaretColor(ACCENT_CYAN);
        field.setFont(FONT_BODY);
        field.setPreferredSize(new Dimension(0, 36));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_SUBTLE, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field != buscarTexto) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                            new LineBorder(ACCENT_CYAN, 1, true),
                            new EmptyBorder(6, 10, 6, 10)
                    ));
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (field != buscarTexto) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                            new LineBorder(BORDER_SUBTLE, 1, true),
                            new EmptyBorder(6, 10, 6, 10)
                    ));
                }
            }
        });
        return field;
    }

    private JButton crearBoton(String texto, Color bg, Color fg, Color borderCol) {
        JButton btn = new JButton(texto);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(FONT_BOLD);
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(borderCol, 1, true));
        btn.setPreferredSize(new Dimension(0, 36));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void actualizarEstadoBotonesFormulario(boolean modoEdicion) {
        if (modoEdicion) {
            agregarButton.setEnabled(false);
            agregarButton.setText("Modo Edición (Usa Modificar)");
            agregarButton.setBackground(BG_INPUT);
            agregarButton.setForeground(TEXT_DIM);
            agregarButton.setBorder(new LineBorder(BORDER_SUBTLE, 1, true));

            modificarButton.setEnabled(true);
            modificarButton.setBackground(ACCENT_CYAN);
            modificarButton.setForeground(BG_OBSIDIAN);

            eliminarButton.setEnabled(true);
        } else {
            agregarButton.setEnabled(true);
            agregarButton.setText("+ Agregar con Portada");
            agregarButton.setBackground(TEXT_WHITE);
            agregarButton.setForeground(BG_OBSIDIAN);
            agregarButton.setBorder(new LineBorder(TEXT_WHITE, 1, true));

            modificarButton.setEnabled(false);
            modificarButton.setBackground(BG_INPUT);
            modificarButton.setForeground(TEXT_DIM);

            eliminarButton.setEnabled(false);
        }
    }

    // ==================== EVENTOS Y OPERACIONES CRUD CON ANTI-DUPLICADOS ====================
    private void configurarEventos() {
        agregarButton.addActionListener(e -> agregarLibro());
        modificarButton.addActionListener(e -> modificarLibro());
        eliminarButton.addActionListener(e -> eliminarLibro());
        limpiarButton.addActionListener(e -> limpiarFormulario());

        btnVistaPortadas.addActionListener(e -> {
            cardLayout.show(centerCardPanel, "PORTADAS");
            btnVistaPortadas.setBackground(TEXT_WHITE);
            btnVistaPortadas.setForeground(BG_OBSIDIAN);
            btnVistaTabla.setBackground(BG_INPUT);
            btnVistaTabla.setForeground(TEXT_MUTED);
        });

        btnVistaTabla.addActionListener(e -> {
            cardLayout.show(centerCardPanel, "TABLA");
            btnVistaTabla.setBackground(TEXT_WHITE);
            btnVistaTabla.setForeground(BG_OBSIDIAN);
            btnVistaPortadas.setBackground(BG_INPUT);
            btnVistaPortadas.setForeground(TEXT_MUTED);
        });

        galeriaLibros.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Libro seleccionado = galeriaLibros.getSelectedValue();
                if (seleccionado != null) {
                    cargarLibroEnInspector(seleccionado);
                }
            }
        });

        tablaLibros.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tablaLibros.getSelectedRow();
                if (row != -1) {
                    int id = Integer.parseInt(tablaModeloLibros.getValueAt(row, 0).toString().replace("#", ""));
                    catalogoCompleto.stream()
                            .filter(l -> l.getIdLibro() != null && l.getIdLibro() == id)
                            .findFirst()
                            .ifPresent(LibroForm.this::cargarLibroEnInspector);
                }
            }
        });

        buscarTexto.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { aplicarFiltros(); }
            @Override
            public void removeUpdate(DocumentEvent e) { aplicarFiltros(); }
            @Override
            public void changedUpdate(DocumentEvent e) { aplicarFiltros(); }
        });
    }

    private void cargarLibroEnInspector(Libro libro) {
        actualizandoCamposProgramaticamente = true;
        idTexto.setText(String.valueOf(libro.getIdLibro()));
        libroTexto.setText(libro.getNombreLibro());
        autorTexto.setText(libro.getAutor());
        precioTexto.setText(String.valueOf(libro.getPrecio()));
        inventarioTexto.setText(String.valueOf(libro.getExistencias()));
        actualizandoCamposProgramaticamente = false;
        popupSugerencias.setVisible(false);

        actualizarEstadoBotonesFormulario(true);

        ImageIcon cover = obtenerPortada(libro);
        Image scaled = cover.getImage().getScaledInstance(68, 96, Image.SCALE_SMOOTH);
        previewCoverLabel.setIcon(new ImageIcon(scaled));
        previewStatusBadge.setText("● Editando SKU #" + String.format("%04d", libro.getIdLibro()));
        previewStatusBadge.setForeground(ACCENT_CYAN);
        previewTituloLabel.setText(libro.getNombreLibro());
        previewAutorLabel.setText(libro.getAutor());
        previewMetaLabel.setText(String.format("SKU #%04d  •  $%.2f", libro.getIdLibro(), libro.getPrecio()));
    }

    private Libro buscarDuplicadoPorTitulo(String titulo, Integer idExcluir) {
        String norm = normalizarTexto(titulo);
        for (Libro l : catalogoCompleto) {
            if (idExcluir != null && idExcluir.equals(l.getIdLibro())) {
                continue;
            }
            if (normalizarTexto(l.getNombreLibro()).equals(norm)) {
                return l;
            }
        }
        return null;
    }

    private String normalizarTexto(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private void agregarLibro() {
        // 1. Bloquear si hay un libro seleccionado actualmente
        if (!idTexto.getText().trim().isEmpty()) {
            mostrarMensaje("Tienes un libro seleccionado en modo edición.\nUsa 'Modificar' para guardar cambios o 'Limpiar Selección' para crear uno nuevo.");
            return;
        }

        String nombreLibro = libroTexto.getText().trim();
        if (nombreLibro.isEmpty()) {
            mostrarMensaje("Proporciona el título del libro.");
            libroTexto.requestFocusInWindow();
            return;
        }

        // 2. Verificar si el libro ya existe en la base de datos para evitar duplicados
        Libro existente = buscarDuplicadoPorTitulo(nombreLibro, null);
        if (existente != null) {
            galeriaLibros.setSelectedValue(existente, true);
            cargarLibroEnInspector(existente);
            mostrarMensaje("El libro '" + existente.getNombreLibro() + "' ya existe en tu biblioteca (SKU #"
                    + String.format("%04d", existente.getIdLibro()) + ").\n"
                    + "Se ha seleccionado automáticamente para que puedas modificar su stock o precio.");
            return;
        }

        try {
            double precio = Double.parseDouble(precioTexto.getText().trim());
            int existencias = Integer.parseInt(inventarioTexto.getText().trim());
            String autor = autorTexto.getText().trim();

            // 3. Si el usuario dejó el autor en blanco, buscar automáticamente el autor real del libro
            if (autor.isEmpty()) {
                List<SugerenciaLibro> sugs = consultarSugerencias(nombreLibro, "");
                if (!sugs.isEmpty() && !sugs.get(0).autor().isEmpty()) {
                    autor = sugs.get(0).autor();
                } else {
                    autor = "Autor Desconocido";
                }
            }

            var libro = new Libro();
            libro.setNombreLibro(nombreLibro);
            libro.setAutor(autor);
            libro.setPrecio(precio);
            libro.setExistencias(existencias);

            this.libroServicio.guardarLibro(libro);
            limpiarFormulario();
            listarLibros();
        } catch (NumberFormatException ex) {
            mostrarMensaje("Verifica que Precio y Existencias sean valores numéricos válidos.");
        }
    }

    private void modificarLibro() {
        if (this.idTexto.getText().isEmpty()) {
            mostrarMensaje("Selecciona una portada o fila para modificarla.");
            return;
        }
        String nombreLibro = libroTexto.getText().trim();
        if (nombreLibro.isEmpty()) {
            mostrarMensaje("El título del libro no puede estar vacío.");
            libroTexto.requestFocusInWindow();
            return;
        }
        try {
            int idLibro = Integer.parseInt(idTexto.getText().trim());

            // Evitar que al renombrar duplique otro libro distinto
            Libro duplicado = buscarDuplicadoPorTitulo(nombreLibro, idLibro);
            if (duplicado != null) {
                mostrarMensaje("Ya existe otro registro con el título '" + duplicado.getNombreLibro()
                        + "' (SKU #" + String.format("%04d", duplicado.getIdLibro()) + ").");
                return;
            }

            String autor = autorTexto.getText().trim();
            if (autor.isEmpty()) {
                List<SugerenciaLibro> sugs = consultarSugerencias(nombreLibro, "");
                autor = (!sugs.isEmpty() && !sugs.get(0).autor().isEmpty()) ? sugs.get(0).autor() : "Autor Desconocido";
            }

            double precio = Double.parseDouble(precioTexto.getText().trim());
            int existencias = Integer.parseInt(inventarioTexto.getText().trim());

            var libro = new Libro(idLibro, nombreLibro, autor, precio, existencias);
            libroServicio.guardarLibro(libro);
            limpiarFormulario();
            listarLibros();
        } catch (NumberFormatException ex) {
            mostrarMensaje("Verifica que Precio y Existencias sean valores numéricos válidos.");
        }
    }

    private void eliminarLibro() {
        if (idTexto.getText().isEmpty()) {
            mostrarMensaje("Selecciona un libro de la galería o tabla para eliminarlo.");
            return;
        }
        int idLibro = Integer.parseInt(idTexto.getText().trim());
        var libro = new Libro();
        libro.setIdLibro(idLibro);
        libroServicio.eliminarLibro(libro);
        limpiarFormulario();
        listarLibros();
    }

    private void limpiarFormulario() {
        actualizandoCamposProgramaticamente = true;
        idTexto.setText("");
        libroTexto.setText("");
        autorTexto.setText("");
        precioTexto.setText("");
        inventarioTexto.setText("");
        actualizandoCamposProgramaticamente = false;
        popupSugerencias.setVisible(false);

        galeriaLibros.clearSelection();
        tablaLibros.clearSelection();
        actualizarEstadoBotonesFormulario(false);

        previewCoverLabel.setIcon(escalarIcono(generarPortadaEditorial("Nuevo Libro", "Autor", 0), 68, 96));
        previewStatusBadge.setText("● Auto-Cover & Autor Web");
        previewStatusBadge.setForeground(STATUS_OK);
        previewTituloLabel.setText("Modo Nuevo Registro");
        previewAutorLabel.setText("Escribe para autocompletar");
        previewMetaLabel.setText("SKU #NUEVO  •  $0.00");
    }

    private void aplicarFiltros() {
        String query = buscarTexto.getText().trim().toLowerCase();
        galeriaModelo.clear();
        tablaModeloLibros.setRowCount(0);

        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);

        for (Libro libro : catalogoCompleto) {
            String titulo = libro.getNombreLibro() != null ? libro.getNombreLibro() : "";
            String autor  = libro.getAutor() != null ? libro.getAutor() : "";
            String sku    = String.format("#%04d", libro.getIdLibro() != null ? libro.getIdLibro() : 0);

            boolean pasaLetra = letraFiltroActiva.equals("TODOS")
                    || (!titulo.isEmpty() && titulo.toUpperCase().startsWith(letraFiltroActiva));

            boolean pasaBusqueda = query.isEmpty()
                    || titulo.toLowerCase().contains(query)
                    || autor.toLowerCase().contains(query)
                    || sku.toLowerCase().contains(query);

            if (pasaLetra && pasaBusqueda) {
                galeriaModelo.addElement(libro);
                tablaModeloLibros.addRow(new Object[]{
                        sku,
                        titulo,
                        autor,
                        currency.format(libro.getPrecio() != null ? libro.getPrecio() : 0.0),
                        (libro.getExistencias() != null ? libro.getExistencias() : 0) + " uds"
                });
            }
        }
        galeriaLibros.revalidate();
        galeriaLibros.repaint();
    }

    private void mostrarMensaje(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Obsidian Library Studio", JOptionPane.INFORMATION_MESSAGE);
    }

    private void listarLibros() {
        catalogoCompleto = libroServicio.ListarLibros();

        int totalUnidades = 0;
        double valorTotal = 0.0;
        int alertas = 0;

        for (Libro l : catalogoCompleto) {
            int stock = l.getExistencias() != null ? l.getExistencias() : 0;
            double precio = l.getPrecio() != null ? l.getPrecio() : 0.0;
            totalUnidades += stock;
            valorTotal += (stock * precio);
            if (stock <= 5) alertas++;

            obtenerPortada(l);
        }

        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
        badgeTotalLibros.setText(" " + catalogoCompleto.size() + " ");
        kpiUnidades.setText(totalUnidades + " uds");
        kpiValor.setText(currency.format(valorTotal));
        kpiAlertas.setText(alertas + (alertas == 1 ? " alerta" : " alertas"));

        aplicarFiltros();
    }
}