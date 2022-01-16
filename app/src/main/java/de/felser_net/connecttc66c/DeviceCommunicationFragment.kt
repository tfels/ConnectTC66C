package de.felser_net.connecttc66c

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import de.felser_net.connecttc66c.databinding.FragmentDeviceCommunicationBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class DeviceCommunicationFragment : Fragment() {

    private var _binding: FragmentDeviceCommunicationBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var btComObject: BluetoothCommunication? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentDeviceCommunicationBinding.inflate(inflater, container, false)

        if(activity is MainActivity) {
            val activity = activity as MainActivity
            btComObject = activity.btComObject
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonGotoBleScan.setOnClickListener {
            findNavController().navigate(R.id.action_DeviceCommunicationFragment_to_BleScanFragment)
        }
        binding.textviewData.text = "<some data>"

        binding.buttonPreviousScreen.setOnClickListener {
            btComObject?.sendCommand(BluetoothCommunication.CMD_PREV_PAGE)
        }
        binding.buttonNextScreen.setOnClickListener {
            btComObject?.sendCommand(BluetoothCommunication.CMD_NEXT_PAGE)
        }
        binding.buttonRotateScreen.setOnClickListener {
            btComObject?.sendCommand(BluetoothCommunication.CMD_ROTATE)
        }
        binding.buttonGetData.setOnClickListener {
            btComObject?.revcData()
            btComObject?.sendCommand(BluetoothCommunication.CMD_GET_VALUES)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()

        // connect
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val deviceAddress = sharedPref.getString(getString(R.string.saved_device_address), null)
        if (deviceAddress != null)
            btComObject?.connect(deviceAddress)
    }

    override fun onPause() {
        btComObject?.disconnect()
        super.onPause()
    }
}