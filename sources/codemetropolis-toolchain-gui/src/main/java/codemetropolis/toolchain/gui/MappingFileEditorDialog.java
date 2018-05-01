package codemetropolis.toolchain.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import codemetropolis.toolchain.gui.beans.BadConfigFileFomatException;

import codemetropolis.toolchain.gui.components.CMButton;
import codemetropolis.toolchain.gui.components.CMLabel;
import codemetropolis.toolchain.gui.components.CMScrollPane;
import codemetropolis.toolchain.gui.components.CMTable;
import codemetropolis.toolchain.gui.components.CMTextField;
import codemetropolis.toolchain.gui.components.listeners.AddResourceListener;
import codemetropolis.toolchain.gui.components.listeners.BrowseListener;
import codemetropolis.toolchain.gui.components.listeners.NewAssigninmentListener;
import codemetropolis.toolchain.gui.components.listeners.RemoveResourceListener;
import codemetropolis.toolchain.gui.components.listeners.SaveMappingListener;
import codemetropolis.toolchain.gui.utils.BuildableSettings;
import codemetropolis.toolchain.gui.utils.Property;
import codemetropolis.toolchain.gui.utils.PropertyCollector;
import codemetropolis.toolchain.gui.utils.TransferHelper;
import codemetropolis.toolchain.gui.utils.Translations;
import codemetropolis.toolchain.gui.utils.XmlFileFilter;

/**
 * Dialog for the mapping file editor.
 * @author Viktor Meszaros {@literal <MEVXAAT.SZE>}
 */
public class MappingFileEditorDialog extends JDialog {
	
	private static final long serialVersionUID = 1L;
	
	private static final FileFilter XML_FILTER;
	
	/**
	 * Contains the possible results of assigning a metric to a property of a buildable type.
	 */
	public enum AssignResult {CANNOT_ASSIGN, NO_CONVERSION, TO_INT, TO_DOUBLE, NORMALIZE, QUANTIZATON};
	
	public static final Map<String, Map<String, AssignResult>> ASSIGN_RESULT_MATRIX;
	
	static {
		XML_FILTER = new XmlFileFilter();

		ASSIGN_RESULT_MATRIX = new HashMap<String, Map<String, AssignResult>>();
		
		ASSIGN_RESULT_MATRIX.put("int", new HashMap<String, AssignResult>());
		ASSIGN_RESULT_MATRIX.put("int(0 to 5)", new HashMap<String, AssignResult>());
		ASSIGN_RESULT_MATRIX.put("string", new HashMap<String, AssignResult>());
		ASSIGN_RESULT_MATRIX.put("float(0 to 1)", new HashMap<String, AssignResult>());
		//First row of the "matrix"
		ASSIGN_RESULT_MATRIX.get("int").put("int", AssignResult.NO_CONVERSION);
		ASSIGN_RESULT_MATRIX.get("int").put("float", AssignResult.TO_INT);
		ASSIGN_RESULT_MATRIX.get("int").put("string", AssignResult.CANNOT_ASSIGN);
		//Second row of the "matrix"
		ASSIGN_RESULT_MATRIX.get("int(0 to 5)").put("int", AssignResult.QUANTIZATON);
		ASSIGN_RESULT_MATRIX.get("int(0 to 5)").put("float", AssignResult.QUANTIZATON);
		ASSIGN_RESULT_MATRIX.get("int(0 to 5)").put("string", AssignResult.CANNOT_ASSIGN);
		//Third row of the "matrix"
		ASSIGN_RESULT_MATRIX.get("string").put("int", AssignResult.QUANTIZATON);
		ASSIGN_RESULT_MATRIX.get("string").put("float", AssignResult.QUANTIZATON);
		ASSIGN_RESULT_MATRIX.get("string").put("string", AssignResult.NO_CONVERSION);
		//Fourth row of the "matrix"
		ASSIGN_RESULT_MATRIX.get("float(0 to 1)").put("int", AssignResult.NORMALIZE);
		ASSIGN_RESULT_MATRIX.get("float(0 to 1)").put("float", AssignResult.NORMALIZE);
		ASSIGN_RESULT_MATRIX.get("float(0 to 1)").put("string", AssignResult.CANNOT_ASSIGN);
	}
		
	private Map<String, String[]> displayedBuildableAttributes;
	private Map<String, List<Property>> sourceCodeElementProperties;
	
	private JTabbedPane buildableTabbedPane;	
	private JPanel cellarPanel;
	private JPanel floorPanel;
	private JPanel gardenPanel;
	private JPanel groundPanel;
	private CMTable cellarTable;	
	private CMTable floorTable;	
	private CMTable gardenTable;
	
	//ListModel and JList for the buildables: cellar, floor, garden
	private ListModel<String> cellarListmodel;
	private JList<String> cellarList;
	private ListModel<String> floorListmodel;
	private JList<String> floorList;
	private ListModel<String> gardenListmodel;
	private JList<String> gardenList;
	
	//ListModel and JList for the resources
	private ListModel<String> resourcesListmodel;
	private JList<String> resourcesList;
	
	private CMTextField pathField;
	
	/**
	 * Loads the list of the buildable attributes which are desired to display on the GUI from the configuration file.
	 * Loads the list of the source code element from the given input cdf xml file.
	 * @param cdfFilePath The path of the input cdf xml file.
	 */
	private void loadDisplayedInfo(String cdfFilePath) {
		try {
			BuildableSettings settings = new BuildableSettings();
			displayedBuildableAttributes = settings.readSettings();						
		}
		catch(BadConfigFileFomatException e) {
			JOptionPane.showMessageDialog(
					null,
					Translations.t("gui_err_invaild_config_file_format"),
					Translations.t("gui_err_title"),
					JOptionPane.ERROR_MESSAGE);
			
			displayedBuildableAttributes = BuildableSettings.DEFAULT_SETTINGS;
		}
		catch(FileNotFoundException e) {
			JOptionPane.showMessageDialog(
					null,
					Translations.t("gui_err_config_file_not_found"),
					Translations.t("gui_err_title"),
					JOptionPane.ERROR_MESSAGE);
			
			displayedBuildableAttributes = BuildableSettings.DEFAULT_SETTINGS;
		}
		try {
			PropertyCollector pc = new PropertyCollector();
			sourceCodeElementProperties = pc.getFromCdf(cdfFilePath);
		}
		catch(FileNotFoundException e) {
			e.printStackTrace();
		}		
	}
	
	 /**
	  * Instantiates the Mapping file editor dialog.
	  * @param cdfFilePath The path of the input cdf xml file.
	  * @param cmGui The parent window of the dialog.
	  */
	public MappingFileEditorDialog(String cdfFilePath, CodeMetropolisGUI cmGui) {
		super(cmGui, Translations.t("gui_mapping_editor_title") ,true);
		loadDisplayedInfo(cdfFilePath);
		
		JPanel panel = createBasePanel();
		addBuildableTabs(panel);
		addResourceOptions(panel);
		addSaveOptions(panel);
		
		
		this.setResizable(false);
	    this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
	    this.setContentPane(panel);
	    this.pack();
	    this.setLocationRelativeTo(cmGui);
	}
	
	/**
	 * Creates the base panel for the Mapping file editor dialog.
	 * 
	 * @return The created {@link JPanel}.
	 */
	private JPanel createBasePanel() {
		JPanel panel = new JPanel();
	    panel.setLayout(null);
	    panel.setBounds(0, 0, 800, 500);

	    Dimension size = new Dimension(800, 500);
	    panel.setMinimumSize(size);
	    panel.setPreferredSize(size);
	    panel.setMaximumSize(size);

	    return panel;
	}
	
	/**
	 * Adds the resource options to the {@code panel}.
	 * @param panel The {@link JPanel} to which the components will be added to.
	 */
	private void addResourceOptions(JPanel panel) {
		CMLabel resourcesLabel = new CMLabel(Translations.t("gui_l_resources"), 10, 0, 120, 30);
		
		resourcesListmodel = new DefaultListModel<String>();
	    resourcesList = new JList<String>(resourcesListmodel);
	    resourcesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    resourcesList.setLayoutOrientation(JList.VERTICAL);
	    resourcesList.setVisibleRowCount(-1);
		CMScrollPane resourcesScrollPane = new CMScrollPane(resourcesList, 10, 35, 240, 120);
		
		List<JList<String>> lists = Arrays.asList(resourcesList, cellarList, floorList, gardenList);
		List<JTable> tables = Arrays.asList(cellarTable, floorTable, gardenTable);
		
		CMButton resourcesAddButton = new CMButton(Translations.t("gui_b_add"), 265, 35, 120, 30);
		resourcesAddButton.addActionListener(new AddResourceListener(lists));
				
		CMButton resourcesRemoveButton = new CMButton(Translations.t("gui_b_remove"), 265, 80, 120, 30);
		resourcesRemoveButton.addActionListener(new RemoveResourceListener(resourcesList, lists, tables));
		
		panel.add(resourcesLabel);
		panel.add(resourcesScrollPane);
		panel.add(resourcesAddButton);
		panel.add(resourcesRemoveButton);
	}
	
	/**
	 * Adds the saving options to the {@code panel}.
	 * @param panel The {@link JPanel} to which the components will be added to.
	 */
	private void addSaveOptions(JPanel panel) {
		CMLabel saveSettingsLabel = new CMLabel(Translations.t("gui_l_save_settings"), 415, 0, 120, 30);
		CMLabel pathLabel = new CMLabel(Translations.t("gui_l_path"), 415, 35, 60, 30);
		pathField = new CMTextField(475, 35, 270, 30);
		CMButton specifyPathButton = new CMButton(Translations.t("gui_b_specify_path"), 415, 80, 120, 30);
		CMButton saveMappingFileButton = new CMButton(Translations.t("gui_b_save_mapping_file"), 415, 120, 165, 30);
		specifyPathButton.addActionListener(new BrowseListener(pathField, Translations.t("gui_save_filechooser_title"), JFileChooser.FILES_ONLY, XML_FILTER));
		List<CMTable> tables = Arrays.asList(cellarTable, floorTable, gardenTable);
		saveMappingFileButton.addActionListener(new SaveMappingListener(pathField, tables, resourcesList));
		
		panel.add(saveSettingsLabel);
		panel.add(pathLabel);
		panel.add(pathField);
		panel.add(specifyPathButton);
		panel.add(saveMappingFileButton);
	}
	
	/**
	 * Adds the the tabs of the buildables to the {@code buildableTabbedPane} {@link JTabbedPane}.
	 * @param panel The {@link JPanel} to which the {@code buildableTabbedPane} will be added to.
	 */
	private void addBuildableTabs(JPanel panel) {
		buildableTabbedPane = new JTabbedPane();
		
		createCellarTab();
		createFloorTab();
		createGardenTab();
		createGroundTab();
		
		buildableTabbedPane.add(Translations.t("gui_tab_cellar"), cellarPanel);
		buildableTabbedPane.add(Translations.t("gui_tab_floor"), floorPanel);
		buildableTabbedPane.add(Translations.t("gui_tab_garden"), gardenPanel);
		buildableTabbedPane.add(Translations.t("gui_tab_ground"), groundPanel);
		
		buildableTabbedPane.setFont(new Font("Source Sans Pro", Font.PLAIN, 16));
		buildableTabbedPane.setBounds(10, 175, 780, 300);
		
		panel.add(buildableTabbedPane);		
	}
	
	/**
	 * Creates the tab to the buildable type cellar, where the buildable attributes and their desired values can be paired.
	 */
	private void createCellarTab() {
		cellarPanel = new JPanel();
		cellarPanel.setLayout(null);
	    cellarPanel.setBounds(0, 0, 780, 285);

	    Dimension size = new Dimension(780, 285);
	    cellarPanel.setMinimumSize(size);
	    cellarPanel.setPreferredSize(size);
	    cellarPanel.setMaximumSize(size);
	    
	    CMLabel assignedLabel = new CMLabel(Translations.t("gui_l_assigned_to"), 15, 15, 270, 30);
	    CMLabel attributeLabel = new CMLabel(Translations.t("gui_l_attribute"), 270, 15, 60, 30);
	    CMLabel propertiesLabel = new CMLabel(Translations.t("gui_l_properties"), 525, 15, 120, 30);
	    
	    cellarTable = setUpBuildableTable("CELLAR");
	    cellarTable.setTarget("cellar");
	    cellarTable.setSource("attribute");
	    Rectangle bounds = cellarTable.getBounds();
	    CMScrollPane scrollPane = new CMScrollPane(cellarTable, bounds.x, bounds.y, bounds.width, bounds.height + 30);
	    
	    cellarListmodel = initializeListModel("attribute");
	    cellarList = new JList<String>();
	    cellarList.setModel(cellarListmodel);
	    cellarList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    cellarList.setLayoutOrientation(JList.VERTICAL);
	    cellarList.setVisibleRowCount(-1);
	    cellarList.setDragEnabled(true);
	    cellarList.setDropMode(DropMode.INSERT);

	    CMScrollPane cellarScrollPane = new CMScrollPane(cellarList, 525, 50, 240, 180);
	    
	    cellarPanel.add(assignedLabel);
	    cellarPanel.add(attributeLabel);
	    cellarPanel.add(propertiesLabel);
	    cellarPanel.add(scrollPane);
	    cellarPanel.add(cellarScrollPane);	    
	}
	
	/**
	 * Creates the tab to the buildable type floor, where the buildable attributes and their desired values can be paired.
	 */
	private void createFloorTab() {
		floorPanel = new JPanel();
		floorPanel.setLayout(null);
	    floorPanel.setBounds(0, 0, 780, 285);

	    Dimension size = new Dimension(780, 285);
	    floorPanel.setMinimumSize(size);
	    floorPanel.setPreferredSize(size);
	    floorPanel.setMaximumSize(size);
	    
	    CMLabel assignedLabel = new CMLabel(Translations.t("gui_l_assigned_to"), 15, 15, 270, 30);
	    CMLabel methodLabel = new CMLabel(Translations.t("gui_l_method"), 270, 15, 60, 30);
	    CMLabel propertiesLabel = new CMLabel(Translations.t("gui_l_properties"), 525, 15, 120, 30);		
	    
	    floorTable = setUpBuildableTable("FLOOR");
	    floorTable.setTarget("floor");
	    floorTable.setSource("method");
	    Rectangle bounds = floorTable.getBounds();
	    CMScrollPane scrollPane = new CMScrollPane(floorTable, bounds.x, bounds.y, bounds.width, bounds.height + 30);
	    
	    floorListmodel = initializeListModel("method");
	    floorList = new JList<String>();
	    floorList.setModel(floorListmodel);
	    floorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    floorList.setLayoutOrientation(JList.VERTICAL);
	    floorList.setVisibleRowCount(-1);
	    floorList.setDragEnabled(true);
	    floorList.setDropMode(DropMode.INSERT);
	    
	    CMScrollPane floorScrollPane = new CMScrollPane(floorList, 525, 50, 240, 180);
	    
	    floorPanel.add(assignedLabel);
	    floorPanel.add(methodLabel);
	    floorPanel.add(propertiesLabel);
	    floorPanel.add(scrollPane);
	    floorPanel.add(floorScrollPane);
	}
	
	/**
	 * Creates the tab to the buildable type garden, where the buildable attributes and their desired values can be paired.
	 */
	private void createGardenTab() {
		gardenPanel = new JPanel();
		gardenPanel.setLayout(null);
	    gardenPanel.setBounds(0, 0, 780, 285);

	    Dimension size = new Dimension(780, 285);
	    gardenPanel.setMinimumSize(size);
	    gardenPanel.setPreferredSize(size);
	    gardenPanel.setMaximumSize(size);
	    
	    CMLabel assignedLabel = new CMLabel(Translations.t("gui_l_assigned_to"), 15, 15, 270, 30);
	    CMLabel classLabel = new CMLabel(Translations.t("gui_l_class"), 270, 15, 60, 30);
	    CMLabel propertiesLabel = new CMLabel(Translations.t("gui_l_properties"), 525, 15, 120, 30);
	    
	    gardenTable = setUpBuildableTable("GARDEN");
	    gardenTable.setTarget("garden");
	    gardenTable.setSource("class");
	    Rectangle bounds = gardenTable.getBounds();
	    CMScrollPane scrollPane = new CMScrollPane(gardenTable, bounds.x, bounds.y, bounds.width, bounds.height + 30);
	    
	    gardenListmodel = initializeListModel("class");
	    gardenList = new JList<String>();
	    gardenList.setModel(gardenListmodel);
	    gardenList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    gardenList.setLayoutOrientation(JList.VERTICAL);
	    gardenList.setVisibleRowCount(-1);
	    gardenList.setDragEnabled(true);
	    gardenList.setDropMode(DropMode.INSERT);
	    
	    CMScrollPane gardenScrollPane = new CMScrollPane(gardenList, 525, 50, 240, 180);
	    
	    gardenPanel.add(assignedLabel);
	    gardenPanel.add(classLabel);
	    gardenPanel.add(propertiesLabel);
	    gardenPanel.add(scrollPane);
	    gardenPanel.add(gardenScrollPane);
	}
	
	/**
	 * Creates the tab to the buildable type ground.
	 */
	private void createGroundTab() {
		groundPanel = new JPanel();
		groundPanel.setLayout(null);
	    groundPanel.setBounds(0, 0, 780, 285);

	    Dimension size = new Dimension(780, 285);
	    groundPanel.setMinimumSize(size);
	    groundPanel.setPreferredSize(size);
	    groundPanel.setMaximumSize(size);
	    
	    CMLabel assignedLabel = new CMLabel(Translations.t("gui_l_assigned_to"), 15, 15, 270, 30);
	    CMLabel packageLabel = new CMLabel(Translations.t("gui_l_package"), 270, 15, 60, 30);
	    CMLabel noAttrsLabel = new CMLabel(Translations.t("gui_l_no_attributes"), 15, 60, 300, 30);
	    
	    groundPanel.add(assignedLabel);
	    groundPanel.add(packageLabel);
	    groundPanel.add(noAttrsLabel);
	}
	
	/**
	 * Sets up the table of a buildable type which contains the attributes of the buildable (height, character, etc.) and provides a second column for their values.
	 * @param buildableType The type of the buildable (method, attribute, etc.).
	 * @return The JTable contains the buildable attributes.
	 */
	private CMTable setUpBuildableTable(String buildableType) {
		CMTable table = new CMTable();
		
		String[] displayedProperties = displayedBuildableAttributes.get(buildableType);
	    
		Object[] header = new String[] {Translations.t("gui_t_attribute"), Translations.t("gui_t_assigned_propery")};
	    Object[][] initData = new Object[displayedProperties.length][2];
	    
	    for(int i = 0; i < displayedProperties.length; i++) {
	    	initData[i][0] = displayedProperties[i] + ": " + BuildableSettings.BUILDABLE_ATTRIBUTE_TYPES.get(displayedProperties[i]);
	    	initData[i][1] = null;
	    }
	    
	    TableModel tModel = new DefaultTableModel(initData, header);
	    table.setModel(tModel);
	    
	    MappingFileEditorDialog self = this;
	    //For listening changes in the table.
	    table.getModel().addTableModelListener(new NewAssigninmentListener(table, self));

	    table.setFont(new Font("Source Sans Pro", Font.PLAIN, 14));
	    table.setRowHeight(30);
	    table.setBounds(15, 50, 480, displayedProperties.length * 30);
        table.setDragEnabled(true);
        table.setDropMode(DropMode.USE_SELECTION);
        table.setTransferHandler(new TransferHelper());
        table.setRowSelectionAllowed(false);
        table.setCellSelectionEnabled(true);

	    return table;
	}

	/**
	 * Fills up the list model of the given source code element type with its own properties/metrics.
	 * @param sourceCodeElementType Type of the source code element (method, attribute, etc.).
	 * @return The {@link ListModel} contains all of the properties/metrics.
	 */
	public ListModel<String> initializeListModel(String sourceCodeElementType) {
		List<Property> propertyList = sourceCodeElementProperties.get(sourceCodeElementType);

		DefaultListModel<String> model = new DefaultListModel<String>();
		
		for(Property p : propertyList) {
			model.addElement(p.name + ": " + p.type);
		}
		
		return model;
	}
}