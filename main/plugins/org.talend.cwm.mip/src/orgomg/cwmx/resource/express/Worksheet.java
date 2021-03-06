/**
 * <copyright> </copyright>
 * 
 * $Id$
 */
package orgomg.cwmx.resource.express;

/**
 * <!-- begin-user-doc --> A representation of the model object '
 * <em><b>Worksheet</b></em>'. <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * This represents a physical Express worksheet.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link orgomg.cwmx.resource.express.Worksheet#isIsTemp <em>Is Temp</em>}</li>
 *   <li>{@link orgomg.cwmx.resource.express.Worksheet#getColumnDimension <em>Column Dimension</em>}</li>
 *   <li>{@link orgomg.cwmx.resource.express.Worksheet#getRowDimension <em>Row Dimension</em>}</li>
 * </ul>
 * </p>
 *
 * @see orgomg.cwmx.resource.express.ExpressPackage#getWorksheet()
 * @model
 * @generated
 */
public interface Worksheet extends orgomg.cwm.objectmodel.core.Class {

    /**
     * Returns the value of the '<em><b>Is Temp</b></em>' attribute. <!--
     * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> If set,
     * this indicates that values in the Worksheet are only temporary, and will
     * be discarded at the end of each Express session. <!-- end-model-doc -->
     * 
     * @return the value of the '<em>Is Temp</em>' attribute.
     * @see #setIsTemp(boolean)
     * @see orgomg.cwmx.resource.express.ExpressPackage#getWorksheet_IsTemp()
     * @model dataType="orgomg.cwm.objectmodel.core.Boolean"
     * @generated
     */
    boolean isIsTemp();

    /**
     * Sets the value of the '{@link orgomg.cwmx.resource.express.Worksheet#isIsTemp <em>Is Temp</em>}' attribute.
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * @param value the new value of the '<em>Is Temp</em>' attribute.
     * @see #isIsTemp()
     * @generated
     */
    void setIsTemp(boolean value);

    /**
     * Returns the value of the '<em><b>Column Dimension</b></em>' reference. It
     * is bidirectional and its opposite is '
     * {@link orgomg.cwmx.resource.express.Dimension#getColumnDimensionInWorksheet
     * <em>Column Dimension In Worksheet</em>}'. <!-- begin-user-doc --> <!--
     * end-user-doc --> <!-- begin-model-doc --> Identifies a Dimension used as
     * the column dimension of the Worksheet. <!-- end-model-doc -->
     * 
     * @return the value of the '<em>Column Dimension</em>' reference.
     * @see #setColumnDimension(Dimension)
     * @see orgomg.cwmx.resource.express.ExpressPackage#getWorksheet_ColumnDimension()
     * @see orgomg.cwmx.resource.express.Dimension#getColumnDimensionInWorksheet
     * @model opposite="columnDimensionInWorksheet"
     * @generated
     */
    Dimension getColumnDimension();

    /**
     * Sets the value of the '{@link orgomg.cwmx.resource.express.Worksheet#getColumnDimension <em>Column Dimension</em>}' reference.
     * <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * @param value the new value of the '<em>Column Dimension</em>' reference.
     * @see #getColumnDimension()
     * @generated
     */
    void setColumnDimension(Dimension value);

    /**
     * Returns the value of the '<em><b>Row Dimension</b></em>' reference. It is
     * bidirectional and its opposite is '
     * {@link orgomg.cwmx.resource.express.Dimension#getRowDimensionInWorksheet
     * <em>Row Dimension In Worksheet</em>}'. <!-- begin-user-doc --> <!--
     * end-user-doc --> <!-- begin-model-doc --> Identifies a Dimension used as
     * the row dimension of the Worksheet. <!-- end-model-doc -->
     * 
     * @return the value of the '<em>Row Dimension</em>' reference.
     * @see #setRowDimension(Dimension)
     * @see orgomg.cwmx.resource.express.ExpressPackage#getWorksheet_RowDimension()
     * @see orgomg.cwmx.resource.express.Dimension#getRowDimensionInWorksheet
     * @model opposite="rowDimensionInWorksheet"
     * @generated
     */
    Dimension getRowDimension();

    /**
     * Sets the value of the '{@link orgomg.cwmx.resource.express.Worksheet#getRowDimension <em>Row Dimension</em>}' reference.
     * <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * @param value the new value of the '<em>Row Dimension</em>' reference.
     * @see #getRowDimension()
     * @generated
     */
    void setRowDimension(Dimension value);

} // Worksheet
