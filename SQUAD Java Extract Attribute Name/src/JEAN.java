// Written by Jean Decian

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import padl.creator.javafile.eclipse.CompleteJavaFileCreator;
import padl.kernel.ICodeLevelModel;
import padl.kernel.IFirstClassEntity;
import padl.kernel.IGhost;
import padl.kernel.exception.CreationException;
import padl.kernel.impl.Factory;
import squad.quality.INominalQualityAttribute;
import squad.quality.INumericQualityAttribute;
import squad.quality.pqmod.PQMODRepository;
import squad.quality.qmood.QMOODRepository;

public class JEAN {
	public static void main(final String[] args) throws IOException, CreationException {
		
		String path = "C:/Users/Jean/Desktop/workspace/"; // main directory with other systems
		String selectedSystem = "exist";
		String root = path + selectedSystem + "/";

		final ICodeLevelModel codeLevelModel = Factory.getInstance().createCodeLevelModel("Model");
		codeLevelModel.create(new CompleteJavaFileCreator(root, ""));
		
		String[] qualityModes = {"pqmod"}; // pqmod, qmood or both
		
		for (String qualityMode : qualityModes) {
			String[] qualities = getQualities(qualityMode);

			String output = path + selectedSystem + "_" + qualityMode + ".csv";
			FileWriter csvWriter = new FileWriter(output);
			csvWriter.append("Entity");
			csvWriter.append(";");
			csvWriter.append(String.join(";", qualities));
			csvWriter.append("\n");
			
			System.out.println("Starting processing");
			
			computeNominalValues(csvWriter, codeLevelModel, qualityMode, qualities);
			
			close(csvWriter);
			
			System.out.println("Processing ended");
		}
	}
	
	public static String[] getQualities(String qualityMode) {
		String[] PQMODQualityAttributes = {
				"Expendability",
				"Generality",
				"Modularity",
				"ModularityAtRuntime",
				"Reusability",
				"Scalability",
				"Understandability"
		};
		
		String[] QMOODQualityAttributes = {
				"Effectiveness",
				"Extendibility",
				"Flexibility",
				"Functionality",
				"Reusability",
				"Understandability"
		};
		
		return (qualityMode == "pqmod") ? PQMODQualityAttributes : QMOODQualityAttributes;
	}
	
	public static void close(FileWriter csvWriter) throws IOException {
		csvWriter.flush();
		csvWriter.close();
	}
	
	public static void computeNominalValues(FileWriter csvWriter, ICodeLevelModel codeLevelModel, String qualityMode, String[] qualities) throws IOException {
		final PQMODRepository PQMODQualityRepository = PQMODRepository.getInstance();
		final QMOODRepository QMOODQualityRepository = QMOODRepository.getInstance();
				
		final Iterator entityIterator = codeLevelModel.getIteratorOnTopLevelEntities();
		
		while (entityIterator.hasNext()) {
			List<String> qualityValues = new ArrayList<>();
			
			final IFirstClassEntity firstClassEntity = (IFirstClassEntity) entityIterator.next();
			
			if (!(firstClassEntity instanceof IGhost)) {
				String firstClassName = firstClassEntity.getDisplayName();
				
				if (!firstClassName.equals("OrientSql")) { // manually ignoring an Entity if it takes too long
					qualityValues.add(firstClassName);
					
					System.out.println("Computing " + qualityMode + " for " + firstClassName);
					
					for (String quality : qualities) {
						if (qualityMode == "pqmod") {
							// compute nominal value
							INominalQualityAttribute qualityAttribute = 
									(INominalQualityAttribute) PQMODQualityRepository.getQualityAttribute(quality);
							
							if (qualityAttribute != null) {
								String nominalValue = qualityAttribute.computeNominalValue(codeLevelModel, firstClassEntity);
								qualityValues.add(nominalValue);
							}
						} else {
							// compute numeric value
							INumericQualityAttribute qualityAttribute =
									(INumericQualityAttribute) QMOODQualityRepository.getQualityAttribute(quality);
							
							if (qualityAttribute != null) {
								double numericValue = qualityAttribute.computeNumericValue(codeLevelModel, firstClassEntity);
								qualityValues.add(String.valueOf(numericValue));
							}
						}
					}
					
					csvWriter.append(String.join(";", qualityValues));
					csvWriter.append("\n");
				}
			}
		}
	}
}