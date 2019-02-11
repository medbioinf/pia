package de.mpc.pia.modeller.execute;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;

import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.execute.xmlparams.CTDTool;
import de.mpc.pia.modeller.execute.xmlparams.CliElementType;
import de.mpc.pia.modeller.execute.xmlparams.CliType;
import de.mpc.pia.modeller.execute.xmlparams.ITEMType;
import de.mpc.pia.modeller.execute.xmlparams.MappingType;
import de.mpc.pia.modeller.execute.xmlparams.NODEType;
import de.mpc.pia.modeller.execute.xmlparams.PARAMETERSType;
import de.mpc.pia.modeller.execute.xmlparams.PossibleITEMType;
import de.mpc.pia.modeller.peptide.PeptideExecuteCommands;
import de.mpc.pia.modeller.protein.ProteinExecuteCommands;
import de.mpc.pia.modeller.psm.PSMExecuteCommands;
import de.mpc.pia.tools.PIAConstants;

public class CTDFileHandler {

	/** logger for this class */
	private static final Logger logger = Logger.getLogger(CTDFileHandler.class);


	/**
	 * Generates the CTD file for XML execution via KNIME and GenericKnimeNode
	 * for the given command in the given path.
	 *
	 * @param pathName
	 * @param command
	 */
	public static <T extends ExecuteModelCommands<?>> void generateCTDFile(
			String pathName, T command, String category) {
		List<List<String>> neededParameters = command.neededXMLParameters();
		if (neededParameters == null) {
			return;
		}

		String toolName = command.prefix() + command.toString();
		String fileName = pathName + File.separator + toolName + ".ctd";

		try {
			// set the basic information
			CTDTool executor = new CTDTool();
			executor.setName(toolName);
			executor.setDescription(command.describe());
			executor.setCategory(category);

			initializeForJavaCommand(executor,
					PIAModeller.class.getCanonicalName(), "pia.jar",
					"-Xmx1G", toolName, command.describe());

			CliType cli = executor.getCli();
			CliElementType cliElement;
			MappingType mapping;

			/*
			<clielement optionIdentifier="-paramFile" required="true" isList="false">
				<mapping referenceName="toolName.paramFile" />
			</clielement>
			 */
			cliElement = new CliElementType();
			cliElement.setOptionIdentifier("-paramFile");
			cliElement.setRequired(true);
			cliElement.setIsList(false);
			mapping = new MappingType();
			mapping.setReferenceName(toolName + ".paramFile");
			cliElement.getMapping().add(mapping);
			cli.getClielement().add(cliElement);

			/*
			<clielement optionIdentifier="-paramOutFile" required="true" isList="false">
				<mapping referenceName="GeneratePipelineXML.paramFile" />
			</clielement>
			 */
			cliElement = new CliElementType();
			cliElement.setOptionIdentifier("-paramOutFile");
			cliElement.setRequired(true);
			cliElement.setIsList(false);
			mapping = new MappingType();
			mapping.setReferenceName(toolName + ".paramOutFile");
			cliElement.getMapping().add(mapping);
			cli.getClielement().add(cliElement);

			/*
			<clielement optionIdentifier="-append" required="true" isList="false" />
			 */
			cliElement = new CliElementType();
			cliElement.setOptionIdentifier("-append");
			cliElement.setRequired(true);
			cliElement.setIsList(false);
			cli.getClielement().add(cliElement);

			/*
			<clielement optionIdentifier="toolName" required="true" isList="false />
			 */
			cliElement = new CliElementType();
			cliElement.setOptionIdentifier(toolName);
			cliElement.setRequired(true);
			cliElement.setIsList(false);
			cli.getClielement().add(cliElement);

			/*
			<clielement optionIdentifier="" required="true" isList="false>
				<mapping referenceName="toolName.ARGUMENT" />
			</clielement>
			 */
			for (List<String> arg : neededParameters) {
				cliElement = new CliElementType();
				cliElement.setOptionIdentifier("");
				cliElement.setRequired(false);
				cliElement.setIsList(false);
				mapping = new MappingType();
				mapping.setReferenceName(toolName + '.' + arg.get(0));
				cliElement.getMapping().add(mapping);
				cli.getClielement().add(cliElement);
			}


			NODEType node = executor.getPARAMETERS().getNODE().get(0);
			ITEMType item;

			/*
			<ITEM name="paramFile"
					type="input-file"
					value=""
					advanced="false"
					required="true"
					description="description"
					supported_formats="*.xml"/>
			 */
			item = new ITEMType();
			item.setName("paramFile");
			item.setType(PossibleITEMType.INPUT_FILE);
			item.setValue("");
			item.setAdvanced(false);
			item.setRequired(true);
			item.setDescription("Path to the file, which contains the build " +
					"pipeline until this step.");
			item.setSupportedFormats("*.xml");
			node.getITEMOrITEMLISTOrNODE().add(item);

			/*
			<ITEM name="paramOutFile"
					type="output-file"
					value=""
					advanced="false"
					required="true"
					description="description"
					supported_formats="*.xml"/>
			 */
			item = new ITEMType();
			item.setType(PossibleITEMType.OUTPUT_FILE);
			item.setName("paramOutFile");
			item.setValue("");
			item.setAdvanced(false);
			item.setRequired(true);
			item.setDescription("Path to the file, which will contain the " +
					"pipeline with added filters.");
			item.setSupportedFormats("*.xml");
			node.getITEMOrITEMLISTOrNODE().add(item);

			/*
			<ITEM name="ARGUMENT"
					type="string"
					value=""
					advanced="false"
					required="true"
					description=""
					restrictions="OTHER_ARGUMENTS" />
			 */
			for (List<String> arg : neededParameters) {
				item = new ITEMType();
				item.setName(arg.get(0));
				item.setType(PossibleITEMType.STRING);
				item.setValue("");
				item.setAdvanced(false);
				item.setRequired(true);
				item.setDescription("");

				if (arg.size() > 1) {
					// add all further arguments to the restrictions
					StringBuilder restrictions = new StringBuilder();
					int c = 0;
					for (String restrict : arg) {
						c++;
						if (c < 2) {
							continue;
						}
						if (c > 2) {
							restrictions.append(',');
						}
						restrictions.append(restrict);
					}
					item.setRestrictions(restrictions.toString());
				}

				node.getITEMOrITEMLISTOrNODE().add(item);
			}

			// write the information to a CTD file
		    JAXBContext context = JAXBContext.newInstance(CTDTool.class);
		    Marshaller m = context.createMarshaller();
		    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		    m.marshal(executor, new File(fileName));
		} catch (JAXBException e) {
			logger.error("Error while writing XML information for " + toolName + '.', e);
		}

	}


	/**
     * Initializes the given {@link CTDTool} for usage as a generic java call
     * of the given class and package.
     *
     * @param tool
     * @param className
     * @param defaultClasspath
     * @param toolName
     */
    public static void initializeForJavaCommand(CTDTool tool, String className,
    		String defaultClasspath, String defaultVMsettings, String toolName,
    		String description) {
    	if (tool != null) {
			tool.setExecutableName("java");
			tool.setVersion(PIAConstants.version);

			// set the cli parameters
			CliType cli = new CliType();
			CliElementType cliElement;
			MappingType mapping;
			tool.setCli(cli);

			/*
			<clielement optionIdentifier="" required="false" isList="false">
				<mapping referenceName="toolName.vmsettings" />
			</clielement>
			 */
			cliElement = new CliElementType();
			cliElement.setOptionIdentifier("");
			cliElement.setRequired(false);
			cliElement.setIsList(false);
			mapping = new MappingType();
			mapping.setReferenceName(toolName + ".vmsettings");
			cliElement.getMapping().add(mapping);
			cli.getClielement().add(cliElement);

			/*
			<clielement optionIdentifier="-cp" required="true" isList="false">
				<mapping referenceName="toolName.classpath" />
			</clielement>
			 */
			cliElement = new CliElementType();
			cliElement.setOptionIdentifier("-cp");
			cliElement.setIsList(false);
			cliElement.setRequired(true);
			mapping = new MappingType();
			mapping.setReferenceName(toolName + ".classpath");
			cliElement.getMapping().add(mapping);
			cli.getClielement().add(cliElement);

			/*
			<clielement optionIdentifier="className" required="true" isList="false"/>
			 */
			cliElement = new CliElementType();
			cliElement.setOptionIdentifier(className);
			cliElement.setIsList(false);
			cliElement.setRequired(true);
			cli.getClielement().add(cliElement);


			// set the parameter information
			PARAMETERSType parameters = new PARAMETERSType();
			parameters.setVersion("1.6.2");
			tool.setPARAMETERS(parameters);

			NODEType node = new NODEType();
			node.setName(toolName);
			node.setDescription(description);
			parameters.getNODE().add(node);

			ITEMType item;

			/*
			<ITEM name="version"
					type="string"
					value="0.1.22"
					advanced="true"
					description="Version of the tool that generated this parameters file."
					required="true" />
			 */
			item = new ITEMType();
			item.setName("version");
			item.setType(PossibleITEMType.STRING);
			item.setValue(PIAConstants.version);
			item.setAdvanced(true);
			item.setDescription("Version of the tool that generated this parameters file.");
			item.setRequired(true);
			node.getITEMOrITEMLISTOrNODE().add(item);

			/*
			<ITEM name="vmsettings"
					type="string"
					value="-Xmx8G"
					advanced="true"
					description="Additional Java settings, like giving Java a big enough amount of memory."
					required="false" />
			 */
			item = new ITEMType();
			item.setName("vmsettings");
			item.setType(PossibleITEMType.STRING);
			item.setValue(defaultVMsettings);
			item.setAdvanced(true);
			item.setDescription("Additional Java settings, like giving Java a big enough amount of memory.");
			item.setRequired(false);
			node.getITEMOrITEMLISTOrNODE().add(item);

			/*
			<ITEM name="classpath"
					type="string"
					value="/home/julian/workspace/packages/pia-runnable.jar"
					advanced="true"
					description="The actual JAR, in which the PIA classes reside."
					required="true" />
			 */
			item = new ITEMType();
			item.setName("classpath");
			item.setType(PossibleITEMType.STRING);
			item.setValue(defaultClasspath);
			item.setAdvanced(true);
			item.setDescription("The actual JAR, in which the PIA classes reside.");
			item.setRequired(false);
			node.getITEMOrITEMLISTOrNODE().add(item);
    	}
    }


	/**
	 * This main writes all automatically generated CTD files in a given path.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		// writes the CTD files for the PSM level
		for (PSMExecuteCommands command : PSMExecuteCommands.values()) {
			generateCTDFile(args[0], command, "PSM");
		}

		// writes the CTD files for the peptide level
		for (PeptideExecuteCommands command : PeptideExecuteCommands.values()) {
			generateCTDFile(args[0], command, "peptide");
		}

		// writes the CTD files for the protein level
		for (ProteinExecuteCommands command : ProteinExecuteCommands.values()) {
			generateCTDFile(args[0], command, "protein");
		}
	}
}
