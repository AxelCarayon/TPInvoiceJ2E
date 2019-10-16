package invoice;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import javax.sql.DataSource;

public class DAO {

	private final DataSource myDataSource;

	/**
	 *
	 * @param dataSource la source de données à utiliser
	 */
	public DAO(DataSource dataSource) {
		this.myDataSource = dataSource;
	}

	/**
	 * Renvoie le chiffre d'affaire d'un client (somme du montant de ses factures)
	 *
	 * @param id la clé du client à chercher
	 * @return le chiffre d'affaire de ce client ou 0 si pas trouvé
	 * @throws SQLException
	 */
	public float totalForCustomer(int id) throws SQLException {
		String sql = "SELECT SUM(Total) AS Amount FROM Invoice WHERE CustomerID = ?";
		float result = 0;
		try (Connection connection = myDataSource.getConnection();
			PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, id); // On fixe le 1° paramètre de la requête
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					result = resultSet.getFloat("Amount");
				}
			}
		}
		return result;
	}

	/**
	 * Renvoie le nom d'un client à partir de son ID
	 *
	 * @param id la clé du client à chercher
	 * @return le nom du client (LastName) ou null si pas trouvé
	 * @throws SQLException
	 */
	public String nameOfCustomer(int id) throws SQLException {
		String sql = "SELECT LastName FROM Customer WHERE ID = ?";
		String result = null;
		try (Connection connection = myDataSource.getConnection();
			PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, id);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					result = resultSet.getString("LastName");
				}
			}
		}
		return result;
	}

	/**
	 * Transaction permettant de créer une facture pour un client
	 *
	 * @param customer Le client
	 * @param productIDs tableau des numéros de produits à créer dans la facture
	 * @param quantities tableau des quantités de produits à facturer faux sinon Les deux tableaux doivent avoir la même
	 * taille
	 * @throws java.lang.Exception si la transaction a échoué
	 */
	public void createInvoice(CustomerEntity customer, int[] productIDs, int[] quantities) throws Exception {  
            String sql1 = "INSERT INTO Invoice(CustomerID) VALUES ? "; //Facture
            String sql2 = "INSERT INTO Item(InvoiceID, Item, ProductID, Quantity, Cost) VALUES (?,?,?,?,?) "; //Objet facturé
            String sql3 = "SELECT Price AS PRIX FROM Product WHERE ID = ?"; //Prix de l'item
            
             try (Connection myConnection = myDataSource.getConnection();
                  PreparedStatement statement1 = myConnection.prepareStatement(sql1, Statement.RETURN_GENERATED_KEYS);
                    PreparedStatement statement2 = myConnection.prepareStatement(sql2);
                    PreparedStatement statement3 = myConnection.prepareStatement(sql3)) {           
            
                myConnection.setAutoCommit(false); //On passe en mode transaction donc on désactive l'autocommit pour pouvoir rollback
                try {
                    statement1.setInt(1, customer.getCustomerId());
                    int numberUpdated1 = statement1.executeUpdate();
                    
                    if (numberUpdated1 == 0) { //la requête n'as rien donné.                        
                        throw new SQLException("Aucune valeur insérée ");
                    }
 
                    ResultSet clefs = statement1.getGeneratedKeys(); //ResultSet car on peut avoir plusieures clefs. 

                    clefs.next(); // On lit la première clef générée
                    int invoiceID = clefs.getInt(1);
                    System.out.println("Première clef : " + invoiceID);

                    
                    // Table Item (InvoiceID, Item, ProductID, Quantity, Cost)             
                    for(int i = 0 ; i < productIDs.length ; i++) {
                        int idProduit = productIDs[i];
                        
                        statement2.setInt(1, invoiceID);
                        statement2.setInt(2, i);  //clef
                        statement2.setInt(3, idProduit);
                        statement2.setInt(4, quantities[i]);
                        
                        //Récupération du prix du produit
                        statement3.setInt(1, idProduit);
                        float prix = 0f;
                        
                        try (ResultSet resultSet = statement3.executeQuery()) {
				if (resultSet.next()) {
                                    prix = resultSet.getFloat("PRIX");
				}
			}
                        
                        statement2.setFloat(5, prix);
                     
                        //Execution requête SQL2
                        int numberUpdated2 = statement2.executeUpdate();
                        if (numberUpdated2 == 0) //pas de modif
                        {
                            throw new SQLException("Aucune valeur insérée");
                        }
                    }
                    
                    myConnection.commit(); //On est arrivé au bout sans erreur, on commit
                } catch (SQLException e) {
                    myConnection.rollback(); // rollback car erreur
                    throw e;
                    
                } finally {
                    myConnection.setAutoCommit(true); //fin de la transaction
                }
            }
	}

	/**
	 *
	 * @return le nombre d'enregistrements dans la table CUSTOMER
	 * @throws SQLException
	 */
	public int numberOfCustomers() throws SQLException {
		int result = 0;

		String sql = "SELECT COUNT(*) AS NUMBER FROM Customer";
		try (Connection connection = myDataSource.getConnection();
			Statement stmt = connection.createStatement()) {
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				result = rs.getInt("NUMBER");
			}
		}
		return result;
	}

	/**
	 *
	 * @param customerId la clé du client à recherche
	 * @return le nombre de bons de commande pour ce client (table PURCHASE_ORDER)
	 * @throws SQLException
	 */
	public int numberOfInvoicesForCustomer(int customerId) throws SQLException {
		int result = 0;

		String sql = "SELECT COUNT(*) AS NUMBER FROM Invoice WHERE CustomerID = ?";

		try (Connection connection = myDataSource.getConnection();
			PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setInt(1, customerId);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				result = rs.getInt("NUMBER");
			}
		}
		return result;
	}

	/**
	 * Trouver un Customer à partir de sa clé
	 *
	 * @param customedID la clé du CUSTOMER à rechercher
	 * @return l'enregistrement correspondant dans la table CUSTOMER, ou null si pas trouvé
	 * @throws SQLException
	 */
	CustomerEntity findCustomer(int customerID) throws SQLException {
		CustomerEntity result = null;

		String sql = "SELECT * FROM Customer WHERE ID = ?";
		try (Connection connection = myDataSource.getConnection();
			PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setInt(1, customerID);

			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				String name = rs.getString("FirstName");
				String address = rs.getString("Street");
				result = new CustomerEntity(customerID, name, address);
			}
		}
		return result;
	}

	/**
	 * Liste des clients localisés dans un état des USA
	 *
	 * @param state l'état à rechercher (2 caractères)
	 * @return la liste des clients habitant dans cet état
	 * @throws SQLException
	 */
	List<CustomerEntity> customersInCity(String city) throws SQLException {
		List<CustomerEntity> result = new LinkedList<>();

		String sql = "SELECT * FROM Customer WHERE City = ?";
		try (Connection connection = myDataSource.getConnection();
			PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setString(1, city);
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					int id = rs.getInt("ID");
					String name = rs.getString("FirstName");
					String address = rs.getString("Street");
					CustomerEntity c = new CustomerEntity(id, name, address);
					result.add(c);
				}
			}
		}
		return result;
	}
}
